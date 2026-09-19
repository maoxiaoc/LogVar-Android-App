#include <jni.h>
#include <vector>
#include <string>
#include "node.h"

extern "C" JNIEXPORT jint JNICALL
Java_app_logvar_NodeRuntime_start(JNIEnv* env, jclass, jobjectArray args) {
  jsize count = env->GetArrayLength(args);
  std::vector<std::string> values;
  std::vector<char*> argv;
  for (jsize i = 0; i < count; i++) {
    auto item = (jstring) env->GetObjectArrayElement(args, i);
    const char* text = env->GetStringUTFChars(item, nullptr);
    values.emplace_back(text);
    env->ReleaseStringUTFChars(item, text);
    env->DeleteLocalRef(item);
  }
  for (auto& value : values) argv.push_back(value.data());
  return node::Start(static_cast<int>(argv.size()), argv.data());
}
