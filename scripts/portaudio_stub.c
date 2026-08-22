#include <stddef.h>
#include <stdint.h>

typedef int PaError;
typedef int PaDeviceIndex;
typedef int PaHostApiIndex;
typedef int PaHostApiTypeId;
typedef double PaTime;
typedef unsigned long PaSampleFormat;
typedef unsigned long PaStreamFlags;
typedef void PaStream;
typedef void PaStreamParameters;
typedef int PaStreamCallback(const void *input, void *output, unsigned long frameCount, const void *timeInfo, unsigned long statusFlags, void *userData);
typedef void PaStreamFinishedCallback(void *userData);

#define paNoError 0
#define paHostApiNotFound -9987
#define paInvalidDevice -9996
#define paNoDevice -1

typedef struct PaVersionInfo {
    int versionMajor;
    int versionMinor;
    int versionSubMinor;
    const char *versionControlRevision;
    const char *versionText;
} PaVersionInfo;

typedef struct PaHostErrorInfo {
    PaHostApiTypeId hostApiType;
    long errorCode;
    const char *errorText;
} PaHostErrorInfo;

typedef struct PaDeviceInfo {
    int structVersion;
    const char *name;
    PaHostApiIndex hostApi;
    int maxInputChannels;
    int maxOutputChannels;
    PaTime defaultLowInputLatency;
    PaTime defaultLowOutputLatency;
    PaTime defaultHighInputLatency;
    PaTime defaultHighOutputLatency;
    double defaultSampleRate;
} PaDeviceInfo;

typedef struct PaHostApiInfo {
    int structVersion;
    PaHostApiTypeId type;
    const char *name;
    int deviceCount;
    PaDeviceIndex defaultInputDevice;
    PaDeviceIndex defaultOutputDevice;
} PaHostApiInfo;

typedef struct PaStreamInfo {
    int structVersion;
    PaTime inputLatency;
    PaTime outputLatency;
    double sampleRate;
} PaStreamInfo;

static PaVersionInfo g_versionInfo = { 19, 7, 0, "19.7.0", "PortAudio V19.7.0-stub" };
static PaHostErrorInfo g_hostErrorInfo = { 0, 0, "No error" };

PaError Pa_Initialize(void) { return paNoError; }
PaError Pa_Terminate(void) { return paNoError; }
int Pa_GetVersion(void) { return 190700; }
const char* Pa_GetVersionText(void) { return "PortAudio V19.7.0-stub"; }
const PaVersionInfo* Pa_GetVersionInfo(void) { return &g_versionInfo; }
const char* Pa_GetErrorText(PaError errorCode) { return "No error"; }
const PaHostErrorInfo* Pa_GetLastHostErrorInfo(void) { return &g_hostErrorInfo; }
PaHostApiIndex Pa_GetHostApiCount(void) { return 0; }
PaHostApiIndex Pa_GetDefaultHostApi(void) { return paHostApiNotFound; }
const PaHostApiInfo* Pa_GetHostApiInfo(PaHostApiIndex hostApi) { return NULL; }
PaHostApiIndex Pa_HostApiTypeIdToHostApiIndex(PaHostApiTypeId type) { return paHostApiNotFound; }
PaDeviceIndex Pa_HostApiDeviceIndexToDeviceIndex(PaHostApiIndex hostApi, int hostApiDeviceIndex) { return paNoDevice; }
PaDeviceIndex Pa_GetDeviceCount(void) { return 0; }
PaDeviceIndex Pa_GetDefaultInputDevice(void) { return paNoDevice; }
PaDeviceIndex Pa_GetDefaultOutputDevice(void) { return paNoDevice; }
const PaDeviceInfo* Pa_GetDeviceInfo(PaDeviceIndex device) { return NULL; }
PaError Pa_IsFormatSupported(const PaStreamParameters *inputParameters, const PaStreamParameters *outputParameters, double sampleRate) { return paInvalidDevice; }
PaError Pa_OpenStream(PaStream** stream, const PaStreamParameters *inputParameters, const PaStreamParameters *outputParameters, double sampleRate, unsigned long framesPerBuffer, PaStreamFlags streamFlags, PaStreamCallback *streamCallback, void *userData) {
    if (stream) *stream = NULL;
    return paInvalidDevice;
}
PaError Pa_OpenDefaultStream(PaStream** stream, int numInputChannels, int numOutputChannels, PaSampleFormat sampleFormat, double sampleRate, unsigned long framesPerBuffer, PaStreamCallback *streamCallback, void *userData) {
    if (stream) *stream = NULL;
    return paInvalidDevice;
}
PaError Pa_CloseStream(PaStream* stream) { return paNoError; }
PaError Pa_SetStreamFinishedCallback(PaStream* stream, PaStreamFinishedCallback* streamFinishedCallback) { return paNoError; }
PaError Pa_StartStream(PaStream *stream) { return paNoError; }
PaError Pa_StopStream(PaStream *stream) { return paNoError; }
PaError Pa_AbortStream(PaStream *stream) { return paNoError; }
PaError Pa_IsStreamStopped(PaStream *stream) { return 1; }
PaError Pa_IsStreamActive(PaStream *stream) { return 0; }
const PaStreamInfo* Pa_GetStreamInfo(PaStream *stream) { return NULL; }
PaTime Pa_GetStreamTime(PaStream *stream) { return 0.0; }
double Pa_GetStreamCpuLoad(PaStream* stream) { return 0.0; }
PaError Pa_ReadStream(PaStream* stream, void *buffer, unsigned long frames) { return paNoError; }
PaError Pa_WriteStream(PaStream* stream, const void *buffer, unsigned long frames) { return paNoError; }
signed long Pa_GetStreamReadAvailable(PaStream* stream) { return 0; }
signed long Pa_GetStreamWriteAvailable(PaStream* stream) { return 0; }
PaError Pa_GetSampleSize(PaSampleFormat format) { return 2; }
void Pa_Sleep(long msec) {}
