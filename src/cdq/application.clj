(ns cdq.application
  (:require [gdl.application :as application]))

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create! 'cdq.application.create/do!
   :dispose! 'cdq.game/dispose
   :render! 'cdq.game/render!
   :resize! 'cdq.game/resize!})

(defn -main []
  (application/start! (reify application/Listener
                        (create! [_]
                          ((requiring-resolve (:create! config))))

                        (dispose! [_]
                          ((requiring-resolve (:dispose! config))))

                        (render! [_]
                          ((requiring-resolve (:render! config))))

                        (resize! [_]
                          ((requiring-resolve (:resize! config)))))
                      config))
