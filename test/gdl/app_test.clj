(ns gdl.app-test
  (:require [gdl.app :as app]))

(defn start-simple-app [listener]
  (app/start {:title "GDL.TEST"
              :width 800
              :height 600
              :fps 60
              :taskbar-icon "moon.png"}
             listener))

(defn -main []
  (start-simple-app (reify app/Listener
                      (create [_]
                        (Thread/sleep 1000)
                        (app/exit))
                      (dispose [_])
                      (render [_])
                      (resize [_ w h]))))
