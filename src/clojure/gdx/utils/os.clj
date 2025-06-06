(ns clojure.gdx.utils.os
  (:import (com.badlogic.gdx.utils Os)))

(def mapping {Os/Android :os/android
              Os/IOS     :os/ios
              Os/Linux   :os/linux
              Os/MacOsX  :os/mac-osx
              Os/Windows :os-windows})
