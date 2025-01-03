(ns cdq.game
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.context :as world :refer [active-entities remove-entity all-entities max-delta-time check-player-input set-camera-on-player-position render-tiled-map update-mouseover-entity update-time tick-potential-fields update-paused-state]]
            [cdq.debug :as debug]
            [cdq.render :as render]
            [clojure.gdx :refer [clear-screen black key-just-pressed? key-pressed?]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [gdl.utils :refer [defsystem install]]
            [gdl.context :as c]
            [gdl.info :as info]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(defsystem destroy)
(defmethod destroy :default [_ eid c])

(defsystem tick)
(defmethod tick :default [_ eid c])

(defn- check-window-hotkeys [c {:keys [controls/window-hotkeys]} stage]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? c (get window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows stage) window-id))))

(defn- close-all-windows [stage]
  (let [windows (group/children (:windows stage))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible % false) windows))))

(defn- check-ui-key-listeners [c {:keys [controls/close-windows-key] :as controls} stage]
  (check-window-hotkeys c controls stage)
  (when (key-just-pressed? c close-windows-key)
    (close-all-windows stage)))

(def ^:private zoom-speed 0.025)

(defn- check-camera-controls [{:keys [gdl.context/world-viewport] :as c}]
  (let [camera (:camera world-viewport)]
    (when (key-pressed? c :minus)  (cam/inc-zoom camera    zoom-speed))
    (when (key-pressed? c :equals) (cam/inc-zoom camera (- zoom-speed)))))

(defn- remove-destroyed-entities [c]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (all-entities c))]
    (remove-entity c eid)
    (doseq [component @eid]
      (destroy component eid c))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [c eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (tick [k v] eid c))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [c]
  (try (run! #(tick-entity c %) (active-entities c))
       (catch Throwable t
         (c/error-window c t)
         #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  c)

(def ^:private ^:dbg-flag pausing? true)

(def close-windows-key :escape)

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn process-frame [c]
  (clear-screen black)
  ; FIXME position DRY
  (set-camera-on-player-position c)
  ; FIXME position DRY
  (render-tiled-map c)
  ; render/entities
  (c/draw-on-world-view c
                        (fn [c]
                          (debug/render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render/entities c)
                          (debug/render-after-entities c)))
  (let [stage (c/stage c)]
    (ui/draw stage c)
    (ui/act  stage c))
  (check-player-input c)
  (let [c (-> c
              update-mouseover-entity
              (update-paused-state pausing?))
        c (if (:cdq.context/paused? c)
            c
            (-> c
                update-time
                tick-potential-fields
                tick-entities))]
    (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
    (check-camera-controls c)
    (check-ui-key-listeners c
                            {:controls/close-windows-key close-windows-key
                             :controls/window-hotkeys    window-hotkeys}
                            (c/stage c))
    c))

; TODO 'info' missing ?

(def entity
  {:optional [#'info/text
              #'entity/create
              #'world/create!
              #'destroy
              #'tick
              #'render/render-below
              #'render/render-default
              #'render/render-above
              #'render/render-info]})

(doseq [[ns-sym k] '{cdq.entity.alert-friendlies-after-duration :entity/alert-friendlies-after-duration
                     cdq.entity.animation :entity/animation
                     cdq.entity.clickable :entity/clickable
                     cdq.entity.delete-after-animation-stopped? :entity/delete-after-animation-stopped?
                     cdq.entity.delete-after-duration :entity/delete-after-duration
                     cdq.entity.destroy-audiovisual :entity/destroy-audiovisual
                     cdq.entity.faction :entity/faction
                     cdq.entity.fsm :entity/fsm
                     cdq.entity.hp :entity/hp
                     cdq.entity.image :entity/image
                     cdq.entity.inventory :entity/inventory
                     cdq.entity.line-render :entity/line-render
                     cdq.entity.mana :entity/mana
                     cdq.entity.modifiers :entity/modifiers
                     cdq.entity.mouseover? :entity/mouseover?
                     cdq.entity.movement :entity/movement
                     cdq.entity.projectile-collision :entity/projectile-collision
                     cdq.entity.skills :entity/skills
                     cdq.entity.species :entity/species
                     cdq.entity.string-effect :entity/string-effect
                     cdq.entity.temp-modifier :entity/temp-modifier}]
  (install entity ns-sym k))

(def entity-state
  (merge-with concat
              entity
              {:optional [#'state/enter
                          #'state/exit
                          #'state/cursor
                          #'state/pause-game?
                          #'state/manual-tick
                          #'state/clicked-inventory-cell
                          #'state/clicked-skillmenu-skill
                          #'state/draw-gui-view]}))

; TODO tests ! - all implemented/wired correctly/etc?
; TODO tests also interesting .... !
; keep running , show green/etc

(doseq [[ns-sym k] '{cdq.entity.state.active-skill :active-skill
                     cdq.entity.state.npc-dead :npc-dead
                     cdq.entity.state.npc-idle :npc-idle
                     cdq.entity.state.npc-moving :npc-moving
                     cdq.entity.state.npc-sleeping :npc-sleeping
                     cdq.entity.state.player-dead :player-dead
                     cdq.entity.state.player-idle :player-idle
                     cdq.entity.state.player-item-on-cursor :player-item-on-cursor
                     cdq.entity.state.player-moving :player-moving
                     cdq.entity.state.stunned :stunned}]
  (install entity-state ns-sym k))

(def effect {:required [#'cdq.effect/applicable?
                        #'cdq.effect/handle]
             :optional [#'info/text
                        #'cdq.effect/useful?
                        #'cdq.effect/render]})

(doseq [[ns-sym k] '{cdq.effect.target-all :effects/target-all
                     cdq.effect.target-entity :effects/target-entity
                     cdq.effect.audiovisual :effects/audiovisual
                     cdq.effect.spawn :effects/spawn
                     cdq.effect.projectile :effects/projectile
                     cdq.effect.sound :effects/sound

                     cdq.effect.target.audiovisual :effects.target/audiovisual
                     cdq.effect.target.convert :effects.target/convert
                     cdq.effect.target.damage :effects.target/damage
                     cdq.effect.target.kill :effects.target/kill
                     cdq.effect.target.melee-damage :effects.target/melee-damage
                     cdq.effect.target.spiderweb :effects.target/spiderweb
                     cdq.effect.target.stun :effects.target/stun}]
  (install effect ns-sym k))
