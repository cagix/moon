(ns cdq.application
  (:require [gdl.application :as application]))

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create!  'cdq.application.create/do!
   :dispose! 'cdq.application.dispose/do!
   :render!  '[cdq.application.render.bind-active-entities/do!
               cdq.application.render.set-camera-on-player/do!
               cdq.application.render.clear-screen/do!
               cdq.application.render.draw-tiled-map/do!
               cdq.application.render.draw-on-world-viewport/do!
               cdq.application.render.draw-ui/do!
               cdq.application.render.update-ui/do!
               cdq.application.render.player-state-handle-click/do!
               cdq.application.render.update-mouseover-entity/do!
               cdq.application.render.bind-paused/do!
               cdq.application.render.when-not-paused/do!
               cdq.application.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
               cdq.application.render.camera-controls/do!]
   :resize!  'cdq.application.resize/do!})

(defn -main []
  (application/start! (reify application/Listener
                        (create! [_]
                          ((requiring-resolve (:create! config))))

                        (dispose! [_]
                          ((requiring-resolve (:dispose! config))))

                        (render! [_]
                          (doseq [f (:render! config)]
                            ((requiring-resolve f))))

                        (resize! [_]
                          ((requiring-resolve (:resize! config)))))
                      config))
