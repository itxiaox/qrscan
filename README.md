# Adnroid二维码/条形码扫描
[![](https://jitpack.io/v/itxiaox/qrscan.svg)](https://jitpack.io/#itxiaox/qrscan)

你的一个star，就是我分享的动力❤️。
----------


   >提供Android 的二维码、条形码的扫描功能
   

## 一. 功能介绍
 

- 二维码扫描识别功能
- 条形码扫描识别功能

## 二. 使用方法

### 1. gradle 引用
 

 - 在根gradle中添加

	
	``` 

		allprojects {
			repositories {
				jcenter()
			   maven { url 'https://jitpack.io' }
			}
		}
	```

   

 -  在module 中添加依赖

	``` nginx
	 implementation 'com.github.itxiaox:qrscan:1.0.0'
	```

### 2. 代码中使用：
	
kotlin代码
``` kotlin
  var captureFragment = CaptureFragment()
            captureFragment.setListener {
                Log.i("test", "onCreate-setListener:$it ")
                captureFragment.dismiss()
                tv_show_result.text = "扫描结果：$it"
            }
            captureFragment.setInputClickListener { captureFragment, msg ->
                Log.i("test", "onCreate-inputClickListener:$msg ")
            }
            captureFragment.show(this@MainActivity.supportFragmentManager,CaptureFragment::class.java.name)
```


## LICENSE

		Copyright 2020 itxiaox

		Licensed under the Apache License, Version 2.0 (the "License");
		you may not use this file except in compliance with the License.
		You may obtain a copy of the License at

		   http://www.apache.org/licenses/LICENSE-2.0

		Unless required by applicable law or agreed to in writing, software
		distributed under the License is distributed on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		See the License for the specific language governing permissions and
		limitations under the License.
