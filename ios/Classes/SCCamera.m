//
//  SCCamera.m
//  Pods-Runner
//
//  Created by Tuluobo on 2020/2/6.
//

#import "SCCamera.h"
#import <AVKit/AVKit.h>
#import <libkern/OSAtomic.h>

@interface SCCamera () <AVCaptureMetadataOutputObjectsDelegate, AVCaptureVideoDataOutputSampleBufferDelegate>

@property(nonatomic, strong) AVCaptureSession *session;
@property(nonatomic, strong) AVCaptureDevice *captureDevice;
@property(nonatomic, strong) AVCaptureDeviceInput *input;
@property(nonatomic, strong) AVCaptureMetadataOutput *metaDataOutput;
@property(nonatomic, strong) AVCaptureVideoDataOutput *videoDataOutput;
@property(readonly) CVPixelBufferRef volatile latestPixelBuffer;

@end

@implementation SCCamera

- (void)dealloc {
    if (_latestPixelBuffer) {
      CFRelease(_latestPixelBuffer);
    }
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        _session = [[AVCaptureSession alloc] init];
        _captureDevice = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
        NSError *error = nil;
        _input = [[AVCaptureDeviceInput alloc] initWithDevice:_captureDevice error:&error];
        if (error) {
            NSAssert(NO, @"----------> AVCaptureDeviceInput init error");
            return nil;
        }
        _metaDataOutput = [AVCaptureMetadataOutput new];
        _videoDataOutput = [AVCaptureVideoDataOutput new];
        [self _setupSession];
    }
    return self;
}

- (void)start {
    [self.session startRunning];
}

- (void)stop {
    [self.session stopRunning];
    for (AVCaptureOutput *output in self.session.outputs) {
        [self.session removeOutput:output];
    }
    for (AVCaptureInput *input in self.session.inputs) {
        [self.session removeInput:input];
    }
}

#pragma mark - Private
- (void)_setupSession {
    if (![self.session canAddInput:self.input] ||
        ![self.session canAddOutput:self.metaDataOutput] ||
        ![self.session canAddOutput:self.videoDataOutput]) {
        return;
    }
    [self.session addInput:self.input];
    // Meta Data Output
    [self.session addOutput:self.metaDataOutput];
    self.metaDataOutput.metadataObjectTypes = self.metaDataOutput.availableMetadataObjectTypes;
    [self.metaDataOutput setMetadataObjectsDelegate:self queue:dispatch_get_main_queue()];
    // Video Data Output
    self.videoDataOutput.videoSettings = @{(NSString *)kCVPixelBufferPixelFormatTypeKey : @(kCVPixelFormatType_32BGRA)};
    [self.videoDataOutput setAlwaysDiscardsLateVideoFrames:YES];
    [self.videoDataOutput setSampleBufferDelegate:self queue:dispatch_get_main_queue()];
    AVCaptureConnection *videoConnection = [AVCaptureConnection connectionWithInputPorts:self.input.ports output:self.videoDataOutput];
    if ([self.captureDevice position] == AVCaptureDevicePositionFront) {
      videoConnection.videoMirrored = YES;
    }
    if ([videoConnection isVideoOrientationSupported]) {
        videoConnection.videoOrientation = AVCaptureVideoOrientationPortrait;
    }
    [self.session addOutputWithNoConnections:self.videoDataOutput];
    [self.session addConnection:videoConnection];
}

#pragma mark - FlutterTexture

- (CVPixelBufferRef _Nullable)copyPixelBuffer {
    CVPixelBufferRef pixelBuffer = _latestPixelBuffer;
    while (!OSAtomicCompareAndSwapPtrBarrier(pixelBuffer, nil, (void **)&_latestPixelBuffer)) {
      pixelBuffer = _latestPixelBuffer;
    }

    return pixelBuffer;
}

#pragma mark - AVCaptureMetadataOutputObjectsDelegate

- (void)captureOutput:(AVCaptureOutput *)output didOutputMetadataObjects:(NSArray<__kindof AVMetadataObject *> *)metadataObjects fromConnection:(AVCaptureConnection *)connection {
    for (AVMetadataObject *metaObject in metadataObjects) {
        AVMetadataMachineReadableCodeObject *obj = (AVMetadataMachineReadableCodeObject *)metaObject;
        if ([obj isMemberOfClass:[AVMetadataMachineReadableCodeObject class]]) {
            if ([self.delegate respondsToSelector:@selector(camera:didScanMetadata:type:)]) {
                [self.delegate camera:self didScanMetadata:obj.stringValue type:obj.type];
            }
        } else {
            NSLog(@"暂时不支持的类型");
        }
    }
}

#pragma mark - AVCaptureVideoDataOutputSampleBufferDelegate

- (void)captureOutput:(AVCaptureOutput *)output didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer fromConnection:(AVCaptureConnection *)connection {
    if (output == self.videoDataOutput) {
        CVPixelBufferRef newBuffer = CMSampleBufferGetImageBuffer(sampleBuffer);
        CFRetain(newBuffer);
        CVPixelBufferRef old = _latestPixelBuffer;
        while (!OSAtomicCompareAndSwapPtrBarrier(old, newBuffer, (void **)&_latestPixelBuffer)) {
            old = _latestPixelBuffer;
        }
        if (old != nil) {
            CFRelease(old);
        }
        if (self.onFrameAvailable) {
          self.onFrameAvailable();
        }
    }
}

@end
