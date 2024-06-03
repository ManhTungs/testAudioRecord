#include <jni.h>
#include "lame.h"

static lame_t lame;

extern "C"
JNIEXPORT void JNICALL
Java_com_yourpackage_AudioConverter_initEncoder(JNIEnv
*env,
jobject thiz, jint
sampleRate,
jint channels, jint
bitrate) {
lame = lame_init();
lame_set_in_samplerate(lame, sampleRate
);
lame_set_num_channels(lame, channels
);
lame_set_brate(lame, bitrate
);
lame_init_params(lame);
}

extern "C"
JNIEXPORT jint
JNICALL
        Java_com_yourpackage_AudioConverter_encode(JNIEnv * env, jobject
thiz,
jshortArray buffer_l, jshortArray
buffer_r,
jint samples, jbyteArray
mp3buf) {
jshort *j_buffer_l = env->GetShortArrayElements(buffer_l, NULL);
jshort *j_buffer_r = env->GetShortArrayElements(buffer_r, NULL);
jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, NULL);

int result = lame_encode_buffer(lame, j_buffer_l, j_buffer_r, samples, (unsigned char *) j_mp3buf,
                                env->GetArrayLength(mp3buf));

env->
ReleaseShortArrayElements(buffer_l, j_buffer_l,
0);
env->
ReleaseShortArrayElements(buffer_r, j_buffer_r,
0);
env->
ReleaseByteArrayElements(mp3buf, j_mp3buf,
0);

return
result;
}

extern "C"
JNIEXPORT jint
JNICALL
        Java_com_yourpackage_AudioConverter_flush(JNIEnv * env, jobject
thiz,
jbyteArray mp3buf
) {
jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, NULL);

int result = lame_encode_flush(lame, (unsigned char *) j_mp3buf, env->GetArrayLength(mp3buf));

env->
ReleaseByteArrayElements(mp3buf, j_mp3buf,
0);

return
result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_yourpackage_AudioConverter_closeEncoder(JNIEnv
*env,
jobject thiz
) {
lame_close(lame);
lame = NULL;
}