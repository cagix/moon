(ns gdl.app-test
  (:require [clojure.gdx.lwjgl :as lwjgl]))

(defn -main []
  (lwjgl/start {:title "Hello World"
                :width 800
                :height 600
                :fps 60
                :taskbar-icon "icon.png"} ; optional
               (reify lwjgl/Application
                 (create [_])
                 (dispose [_])
                 (render [_])
                 (resize [_ w h]))))
