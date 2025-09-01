(ns cdq.start
  (:require cdq.application
            cdq.game
            cdq.multifn
            cdq.gdx.backends.lwjgl
            cdq.java.awt
            cdq.render-layers
            cdq.entity.alert-friendlies-after-duration
            cdq.entity.animation
            cdq.entity.body
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.destroy-audiovisual
            cdq.entity.fsm
            cdq.entity.inventory
            cdq.entity.projectile-collision
            cdq.entity.movement
            cdq.entity.skills
            cdq.entity.stats
            cdq.entity.state.active-skill
            cdq.entity.state.npc-idle
            cdq.entity.state.npc-moving
            cdq.entity.state.npc-sleeping
            cdq.entity.state.stunned
            cdq.entity.string-effect
            cdq.entity.temp-modifier
            [clojure.java.io :as io])
  (:gen-class))

(defn- install-entity-components! []
  (.bindRoot #'cdq.ctx.world/entity-components
             {:entity/animation                       {:create   cdq.entity.animation/create}
              :entity/body                            {:create   cdq.entity.body/create}
              :entity/delete-after-animation-stopped? {:create!  cdq.entity.delete-after-animation-stopped/create!}
              :entity/delete-after-duration           {:create   cdq.entity.delete-after-duration/create}
              :entity/projectile-collision            {:create   cdq.entity.projectile-collision/create}
              :creature/stats                         {:create   cdq.entity.stats/create}
              :entity/fsm                             {:create!  cdq.entity.fsm/create!}
              :entity/inventory                       {:create!  cdq.entity.inventory/create!}
              :entity/skills                          {:create!  cdq.entity.skills/create!}
              :entity/destroy-audiovisual             {:destroy! cdq.entity.destroy-audiovisual/destroy!}})

  (.bindRoot #'cdq.game/entity->tick
             {:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick!
              :entity/animation cdq.entity.animation/tick!
              :entity/delete-after-animation-stopped? cdq.entity.delete-after-animation-stopped/tick!
              :entity/delete-after-duration cdq.entity.delete-after-duration/tick!
              :entity/movement cdq.entity.movement/tick!
              :entity/projectile-collision cdq.entity.projectile-collision/tick!
              :entity/skills cdq.entity.skills/tick!
              :active-skill cdq.entity.state.active-skill/tick!
              :npc-idle cdq.entity.state.npc-idle/tick!
              :npc-moving cdq.entity.state.npc-moving/tick!
              :npc-sleeping cdq.entity.state.npc-sleeping/tick!
              :stunned cdq.entity.state.stunned/tick!
              :entity/string-effect cdq.entity.string-effect/tick!
              :entity/temp-modifier cdq.entity.temp-modifier/tick!}))

(defn- install-effects! []
  (cdq.multifn/add-methods! '[{:required [cdq.world.effect/applicable?
                                          cdq.world.effect/handle]
                               :optional [cdq.world.effect/useful?
                                          cdq.world.effect/render]}
                              [[cdq.effects.audiovisual
                                :effects/audiovisual]
                               [cdq.effects.projectile
                                :effects/projectile]
                               [cdq.effects.sound
                                :effects/sound]
                               [cdq.effects.spawn
                                :effects/spawn]
                               [cdq.effects.target-all
                                :effects/target-all]
                               [cdq.effects.target-entity
                                :effects/target-entity]
                               [cdq.effects.target.audiovisual
                                :effects.target/audiovisual]
                               [cdq.effects.target.convert
                                :effects.target/convert]
                               [cdq.effects.target.damage
                                :effects.target/damage]
                               [cdq.effects.target.kill
                                :effects.target/kill]
                               [cdq.effects.target.melee-damage
                                :effects.target/melee-damage]
                               [cdq.effects.target.spiderweb
                                :effects.target/spiderweb]
                               [cdq.effects.target.stun
                                :effects.target/stun]]]))

(defn- install-txs! []
  (cdq.multifn/add-methods! '[{:required [cdq.game/do!]}
                              [[cdq.tx.toggle-inventory-visible
                                :tx/toggle-inventory-visible]
                               [cdq.tx.show-message
                                :tx/show-message]
                               [cdq.tx.show-modal
                                :tx/show-modal]
                               [cdq.tx.sound
                                :tx/sound]
                               [cdq.tx.state-exit
                                :tx/state-exit]
                               [cdq.tx.state-enter
                                :tx/state-enter]
                               [cdq.tx.audiovisual
                                :tx/audiovisual]
                               [cdq.tx.spawn-alert
                                :tx/spawn-alert]
                               [cdq.tx.spawn-line
                                :tx/spawn-line]
                               [cdq.tx.deal-damage
                                :tx/deal-damage]
                               [cdq.tx.set-movement
                                :tx/set-movement]
                               [cdq.tx.move-entity
                                :tx/move-entity]
                               [cdq.tx.spawn-projectile
                                :tx/spawn-projectile]
                               [cdq.tx.spawn-effect
                                :tx/spawn-effect]
                               [cdq.tx.spawn-item
                                :tx/spawn-item]
                               [cdq.tx.spawn-creature
                                :tx/spawn-creature]]]))

(defn- install-editor-widgets! []
  (run! require '[cdq.ui.editor.widget.default
                  cdq.ui.editor.widget.edn
                  cdq.ui.editor.widget.string
                  cdq.ui.editor.widget.boolean
                  cdq.ui.editor.widget.enum
                  cdq.ui.editor.widget.sound
                  cdq.ui.editor.widget.one-to-one
                  cdq.ui.editor.widget.one-to-many
                  cdq.ui.editor.widget.image
                  cdq.ui.editor.widget.animation
                  cdq.ui.editor.widget.map]))

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

(defn -main []
  (install-entity-components!)
  (install-effects!)
  (install-txs!)
  (install-editor-widgets!)
  (cdq.java.awt/set-taskbar-icon! (io/resource "icon.png"))
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
