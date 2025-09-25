(ns clojure.gdx.application-test
  (:require [gdl.application.desktop :as lwjgl3-application]))

(defn -main []
  (lwjgl3-application/start!
   {:listener (reify lwjgl3-application/Listener
                (create [_ context]
                  (println"create!"))
                (dispose [_]
                  (println "dispose!"))
                (pause [_])
                (render [_]
                  #_(println "render!"))
                (resize [_ width height]
                  (println "resize!"))
                (resume [_]))
    :config {:title "Fooz Baaz"
             :windowed-mode {:width 800
                             :height 600}
             :foreground-fps 60
             :mac {:glfw-async? true
                   :taskbar-icon nil}}}))
