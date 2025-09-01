(ns cdq.start.gdx-app
  (:require cdq.application
            cdq.game
            cdq.gdx.backends.lwjgl
            cdq.render-layers))

(def config
  {:cdq.game/stage {:skin-scale :x1}
   :cdq.game/audio {:sounds "sounds.edn"
                    :path-format "sounds/%s.wav"}
   :cdq.game/db {:schemas "schema.edn"
                 :properties "properties.edn"}
   :cdq.game/starting-level [(requiring-resolve 'cdq.level.from-tmx/create)
                             {:tmx-file "maps/vampire.tmx"
                              :start-position [32 71]}]
   :cdq.game/graphics
   {:colors [["PRETTY_NAME" [0.84 0.8 0.52 1]]]
    :tile-size 48
    :ui-viewport    {:width 1440 :height 900}
    :world-viewport {:width 1440 :height 900}
    :cursor-path-format "cursors/%s.png"
    :cursors {:cursors/bag                   ["bag001"       [0   0]]
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
              :cursors/walking               ["walking"      [16 16]]}
    :default-font {:file "exocet/films.EXL_____.ttf"
                   :params {:size 16
                            :quality-scaling 2
                            :enable-markup? true
                            ; false, otherwise scaling to world-units not visible
                            :use-integer-positions? false}}}
   :cdq.game/config {:cdq.ctx.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                                  :initial-state :npc-sleeping}
                                                     :entity/faction :evil}
                     :cdq.ctx.game/player-props {:creature-id :creatures/vampire
                                                 :components {:entity/fsm {:fsm :fsms/player
                                                                           :initial-state :player-idle}
                                                              :entity/faction :good
                                                              :entity/player? true
                                                              :entity/free-skill-points 3
                                                              :entity/clickable {:type :clickable/player}
                                                              :entity/click-distance-tiles 1.5}}
                     :cdq.game/world {:content-grid-cell-size 16
                                      :potential-field-factions-iterations {:good 15
                                                                            :evil 5}}
                     :effect-body-props {:width 0.5
                                         :height 0.5
                                         :z-order :z-order/effect}

                     :controls {:zoom-in :minus
                                :zoom-out :equals
                                :unpause-once :p
                                :unpause-continously :space}
                     :cdq.game/render-layers [{:entity/mouseover? cdq.render-layers/draw-mouseover-highlighting
                                               :stunned cdq.render-layers/draw-stunned-state
                                               :player-item-on-cursor cdq.render-layers/draw-item-on-cursor-state}
                                              {:entity/clickable cdq.render-layers/draw-clickable-mouseover-text
                                               :entity/animation cdq.render-layers/call-render-image
                                               :entity/image cdq.render-layers/draw-centered-rotated-image
                                               :entity/line-render cdq.render-layers/draw-line-entity}
                                              {:npc-sleeping cdq.render-layers/draw-sleeping-state
                                               :entity/temp-modifier cdq.render-layers/draw-temp-modifiers
                                               :entity/string-effect cdq.render-layers/draw-text-over-entity}
                                              {:creature/stats cdq.render-layers/draw-stats
                                               :active-skill cdq.render-layers/draw-active-skill}]}})

(defn do! []
  (cdq.gdx.backends.lwjgl/start-application!
   {:title "Cyber Dungeon Quest"
    :windowed-mode {:width 1440 :height 900}
    :foreground-fps 60}
   {:create! (fn [gdx]
               (reset! cdq.application/state (cdq.game/create! gdx config)))
    :dispose! (fn []
                (cdq.game/dispose! @cdq.application/state))
    :render! (fn []
               (swap! cdq.application/state cdq.game/render!))
    :resize! (fn [width height]
               (cdq.game/resize! @cdq.application/state width height))}))
