(ns cdq.application.edn
  (:require cdq.application
            cdq.effects
            cdq.impl.entity.state
            cdq.schemas
            cdq.platform.libgdx
            clojure.gdx.application
            clojure.gdx.backends.lwjgl
            clojure.java.io))

(defn -main []
  (.setIconImage (java.awt.Taskbar/getTaskbar)
                 (.getImage (java.awt.Toolkit/getDefaultToolkit)
                            (clojure.java.io/resource "moon.png")))
  (when (= com.badlogic.gdx.utils.SharedLibraryLoader/os
           com.badlogic.gdx.utils.Os/MacOsX)
    (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (clojure.gdx.backends.lwjgl/application (reify clojure.gdx.application/Listener
                                            (create [_]
                                              (cdq.application/create '[[:cdq/files    (cdq.gdx/files)]
                                                                        [:cdq/graphics (cdq.gdx/graphics)]
                                                                        [:cdq/input    (cdq.gdx/input)]
                                                                        [:cdq/schemas      (cdq.schemas/load-from-edn "schema.edn")]
                                                                        [:cdq/db       (cdq.impl.db/create "properties.edn")]
                                                                        [:cdq/assets   (cdq.impl.assets/manager)]
                                                                        [:cdq.graphics/batch (cdq.gdx.graphics/sprite-batch)]
                                                                        [:cdq.graphics/shape-drawer-texture (cdq.graphics/white-pixel-texture)]
                                                                        [:cdq.graphics/shape-drawer (cdq.gdx.graphics.shape-drawer/create)]
                                                                        [:cdq.graphics/cursors (cdq.graphics/cursors {:cursors/bag                   ["bag001"       [0   0]]
                                                                                                                      :cursors/black-x               ["black_x"      [0   0]]
                                                                                                                      :cursors/default               ["default"      [0   0]]
                                                                                                                      :cursors/denied                ["denied"       [16 16]]
                                                                                                                      :cursors/hand-before-grab      ["hand004"      [4  16]]
                                                                                                                      :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                                                                                                      :cursors/hand-grab             ["hand003"      [4  16]]
                                                                                                                      :cursors/move-window           ["move002"      [16 16]]
                                                                                                                      :cursors/no-skill-selected     ["denied003"    [0   0]]
                                                                                                                      :cursors/over-button           ["hand002"      [0   0]]
                                                                                                                      :cursors/sandclock             ["sandclock"    [16 16]]
                                                                                                                      :cursors/skill-not-usable      ["x007"         [0   0]]
                                                                                                                      :cursors/use-skill             ["pointer004"   [0   0]]
                                                                                                                      :cursors/walking               ["walking"      [16 16]]})]
                                                                        [:cdq.graphics/default-font (cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                                                                       :size 16
                                                                                                                                       :quality-scaling 2})]
                                                                        [:cdq.graphics/world-unit-scale (cdq.graphics.world-unit-scale/create 48)]
                                                                        [:cdq.graphics/tiled-map-renderer (cdq.graphics.tiled-map-renderer/create)]
                                                                        [:cdq.graphics/ui-viewport (cdq.graphics.ui-viewport/create {:width 1440 :height 900})]
                                                                        [:cdq.graphics/world-viewport (cdq.graphics.world-viewport/create {:width 1440 :height 900})]
                                                                        [:cdq.context/stage (cdq.ui/setup-stage! {:skin-scale :x1
                                                                                                                  :actors [(cdq.ui.dev-menu/create)
                                                                                                                           (cdq.ui.actionbar/create)
                                                                                                                           (cdq.ui.hp-mana-bar/create)
                                                                                                                           (cdq.ui.windows/create [(cdq.ui.entity-info-window/create)
                                                                                                                                                   (cdq.widgets.inventory/create)])
                                                                                                                           (cdq.ui.player-state/create)
                                                                                                                           (cdq.ui.player-message/actor)]})]
                                                                        [:cdq.context/elapsed-time (cdq.timer/create-ctx)]
                                                                        [:cdq.context/player-message (cdq.ui.player-message/create* {:duration-seconds 1.5})]
                                                                        [:cdq.context/level (cdq.level/create :worlds/uf-caves)]
                                                                        [:cdq.context/error (cdq.create/error*)]
                                                                        [:cdq.context/tiled-map (cdq.create/tiled-map*)]
                                                                        [:cdq.context/explored-tile-corners (cdq.world.explored-tile-corners/create)]
                                                                        [:cdq.context/grid (cdq.world.grid/create)]
                                                                        [:cdq.context/raycaster (cdq.world.raycaster/create)]
                                                                        [:cdq.context/content-grid (cdq.world.content-grid/create {:cell-size 16})]
                                                                        [:cdq.context/entity-ids (cdq.create/entity-ids*)]
                                                                        [:cdq.context/factions-iterations (cdq.create/factions-iterations* {:good 15 :evil 5})]
                                                                        [:world/potential-field-cache (cdq.potential-fields/create-cache)]
                                                                        [:cdq.context/player-eid (cdq.world.entities/spawn)]]))

                                            (dispose [_]
                                              (cdq.application/dispose))

                                            (pause [_])

                                            (render [_]
                                              (cdq.application/render '[(cdq.content-grid/assoc-active-entities)
                                                                        (cdq.render.camera/set-on-player)
                                                                        (cdq.gdx.graphics/clear-screen)
                                                                        (cdq.render.tiled-map/draw)
                                                                        (cdq.graphics/draw-on-world-view [(cdq.render.before-entities/render)
                                                                                                          (cdq.world.graphics/render-entities
                                                                                                           {:below {:entity/mouseover? cdq.world.graphics/draw-faction-ellipse
                                                                                                                    :player-item-on-cursor cdq.world.graphics/draw-world-item-if-exists
                                                                                                                    :stunned cdq.world.graphics/draw-stunned-circle}
                                                                                                            :default {:entity/image cdq.world.graphics/draw-image-as-of-body
                                                                                                                      :entity/clickable cdq.world.graphics/draw-text-when-mouseover-and-text
                                                                                                                      :entity/line-render cdq.world.graphics/draw-line}
                                                                                                            :above {:npc-sleeping cdq.world.graphics/draw-zzzz
                                                                                                                    :entity/string-effect cdq.world.graphics/draw-text
                                                                                                                    :entity/temp-modifier cdq.world.graphics/draw-filled-circle-grey}
                                                                                                            :info {:entity/hp cdq.world.graphics/draw-hpbar-when-mouseover-and-not-full
                                                                                                                   :active-skill cdq.world.graphics/draw-skill-image-and-active-effect}})
                                                                                                          (cdq.render.after-entities/render)])
                                                                        (cdq.render/draw-stage)
                                                                        (cdq.render/update-stage)
                                                                        (cdq.render/player-state-input)
                                                                        (cdq.render/update-mouseover-entity)
                                                                        (cdq.render/update-paused)
                                                                        (cdq.render/when-not-paused)
                                                                        (cdq.render/remove-destroyed-entities)
                                                                        (cdq.render/camera-controls)
                                                                        (cdq.render/window-controls)]))

                                            (resize [_ width height]
                                              (cdq.application/resize '(cdq.graphics/resize-viewports) width height))

                                            (resume [_]))
                                          {:title "Cyber Dungeon Quest"
                                           :windowed-mode {:width 1440
                                                           :height 900}
                                           :foreground-fps 60
                                           :opengl-emulation {:gl-version :gl20
                                                              :gles-3-major-version 3
                                                              :gles-3-minor-version 2}}))
