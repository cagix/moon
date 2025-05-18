(ns cdq.application
  (:require [gdl.application :as application]))

(def config
  {:title "Cyber Dungeon Quest"
   :window-width 1440
   :window-height 900
   :fps 60
   :dock-icon "moon.png"
   :create!  '[cdq.application.create.config/do!
               cdq.application.create.requires/do!
               cdq.application.create.schemas/do!
               cdq.application.create.db/do!
               cdq.application.create.assets/do!
               cdq.application.create.batch/do!
               cdq.application.create.shape-drawer-texture/do!
               cdq.application.create.shape-drawer/do!
               cdq.application.create.cursors/do!
               cdq.application.create.default-font/do!
               cdq.application.create.world-unit-scale/do!
               cdq.application.create.world-viewport/do!
               cdq.application.create.tiled-map-renderer/do!
               cdq.application.create.ui-viewport/do!
               cdq.application.create.ui/do!
               cdq.application.create.game-state/do!]
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
                          (doseq [f (:create! config)]
                            ((requiring-resolve f))))

                        (dispose! [_]
                          ((requiring-resolve (:dispose! config))))

                        (render! [_]
                          (doseq [f (:render! config)]
                            ((requiring-resolve f))))

                        (resize! [_]
                          ((requiring-resolve (:resize! config)))))
                      config))
