# MyAndroidThingsProject
The project using android things to collect data from device and send the data to android pad via Bluetooth, and send data to server via Mqtt protocol.

//About MQTT configuration
添加依赖

repositories {
    google()
    jcenter()
    maven {
        url "https://repo.eclipse.org/content/repositories/paho-releases/"
    }
}

注意下面的版本，版本号不对会失败。
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

之后注册服务：
<service android:name="org.eclipse.paho.android.service.MqttService" />












