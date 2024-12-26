(ns gdl.app-test
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl3]))

(defn -main []
  (lwjgl3/start {:title "Hello World"
                 :width 800
                 :height 600
                 :fps 60
                 :taskbar-icon "icon.png"} ; optional
                (reify lwjgl3/Application
                  (create [_])
                  (dispose [_])
                  (render [_])
                  (resize [_ w h]))))
