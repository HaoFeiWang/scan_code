//
//  SCCamera.h
//  Pods-Runner
//
//  Created by Tuluobo on 2020/2/6.
//

#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>
#import <AVKit/AVKit.h>

NS_ASSUME_NONNULL_BEGIN

@class SCCamera;
@protocol SCCameraDelegate <NSObject>

- (void)camera:(SCCamera *)camera didScanMetadata:(NSString *)metaData type:(AVMetadataObjectType)type;

@end

@interface SCCamera : NSObject <FlutterTexture>

@property(nonatomic, weak) id<SCCameraDelegate> delegate;
@property(nonatomic, copy) void (^onFrameAvailable)(void);

- (void)start;
- (void)stop;

@end

NS_ASSUME_NONNULL_END
