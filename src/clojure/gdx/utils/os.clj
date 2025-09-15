(ns clojure.gdx.utils.os
  (:import (com.badlogic.gdx.utils Os)))

(def ->clj
  {Os/Android :android
   Os/IOS     :ios
   Os/Linux   :linux
   Os/MacOsX  :mac
   Os/Windows :windows})
