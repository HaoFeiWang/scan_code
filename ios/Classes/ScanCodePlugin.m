#import "ScanCodePlugin.h"
#import "SCCamera.h"

static NSString *const kScanCodeMethodChannelKey = @"com.whf.plugin/scan_code";
static NSString *const kScanCodeEventChannelKey = @"com.whf.plugin/scan_code/event";

@interface ScanCodePlugin() <SCCameraDelegate, FlutterStreamHandler>

@property(nonatomic, strong) SCCamera *camera;
@property(nonatomic, strong) NSObject<FlutterTextureRegistry> *registry;
@property(nonatomic, strong) NSObject<FlutterBinaryMessenger> *messenger;
@property(nonatomic, copy) FlutterEventSink eventSink;

@end

@implementation ScanCodePlugin

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* methodChannel = [FlutterMethodChannel methodChannelWithName:kScanCodeMethodChannelKey binaryMessenger:[registrar messenger]];
    ScanCodePlugin* instance = [[ScanCodePlugin alloc] initWithRegistry:[registrar textures] messenger:[registrar messenger]];
    [registrar addMethodCallDelegate:instance channel:methodChannel];
    FlutterEventChannel *eventChannel = [FlutterEventChannel eventChannelWithName:kScanCodeEventChannelKey binaryMessenger:[registrar messenger]];
    [eventChannel setStreamHandler:instance];
}

- (instancetype)initWithRegistry:(NSObject<FlutterTextureRegistry> *)registry messenger:(NSObject<FlutterBinaryMessenger> *)messenger {
    self = [super init];
    if (self) {
        _registry = registry;
        _messenger = messenger;
    }
    return self;
}
    
- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"startScan" isEqualToString:call.method]) {
        [self scanBarCodeWithCall:call result:result];
    } else if ([@"stopScan" isEqualToString:call.method]) {
        [self.camera stop];
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)scanBarCodeWithCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    self.camera = [[SCCamera alloc] init];
    self.camera.delegate = self;
    int64_t textureId = [self.registry registerTexture:self.camera];
    __weak typeof(self) weakSelf = self;
    self.camera.onFrameAvailable = ^{
        __strong typeof(weakSelf) strongSelf = weakSelf;
      [strongSelf.registry textureFrameAvailable:textureId];
    };
    [self.camera start];
    result(@(textureId));
}

#pragma mark - FlutterStreamHandler

- (FlutterError *)onListenWithArguments:(id)arguments eventSink:(FlutterEventSink)events {
    self.eventSink = events;
    return nil;
}

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    self.eventSink = nil;
    return nil;
}

#pragma mark - SCCameraDelegate

- (void)camera:(SCCamera *)camera didScanMetadata:(NSString *)metaData type:(AVMetadataObjectType)type {
    self.eventSink(metaData);
}

@end
