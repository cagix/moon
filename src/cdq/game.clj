(ns cdq.game
  (:require [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.context :as world :refer [line-of-sight? render-z-order active-entities point->entities active-entities remove-entity all-entities max-delta-time]]
            [cdq.debug :as debug]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.potential-fields :as potential-fields]
            [gdl.component :refer [defsystem install]]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx :refer [clear-screen black key-just-pressed? key-pressed?]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.java.io :as io]
            [gdl.utils :refer [sort-by-order]]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.error :refer [pretty-pst]]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]))

(defsystem destroy)
(defmethod destroy :default [_ eid c])

(defsystem tick)
(defmethod tick :default [_ eid c])

(defsystem render-below)
(defmethod render-below :default [_ entity c])

(defsystem render-default)
(defmethod render-default :default [_ entity c])

(defsystem render-above)
(defmethod render-above :default [_ entity c])

(defsystem render-info)
(defmethod render-info :default [_ entity c])

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

(defn- check-camera-controls [c camera]
  (when (key-pressed? c :minus)  (cam/inc-zoom camera    zoom-speed))
  (when (key-pressed? c :equals) (cam/inc-zoom camera (- zoom-speed))) )

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

(def ^:private pf-cache (atom nil))

(defn- tick-potential-fields [{:keys [cdq.context/factions-iterations
                                      cdq.context/grid] :as c}]
  (let [entities (active-entities c)]
    (doseq [[faction max-iterations] factions-iterations]
      (potential-fields/tick pf-cache
                             grid
                             faction
                             entities
                             max-iterations)))
  c)

(defn- update-time [c]
  (let [delta-ms (min (gdx/delta-time c) max-delta-time)]
    (-> c
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn- update-paused-state [{:keys [cdq.context/player-eid error] :as c} pausing?]
  (assoc c :cdq.context/paused? (or error
                                    (and pausing?
                                         (state/pause-game? (entity/state-obj @player-eid))
                                         (not (or (key-just-pressed? c :p)
                                                  (key-pressed? c :space)))))))

(defn- calculate-mouseover-eid [{:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities c (c/world-mouse-position c)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn- update-mouseover-entity [{:keys [cdq.context/mouseover-eid] :as c}]
  (let [new-eid (if (c/mouse-on-actor? c)
                  nil
                  (calculate-mouseover-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [c entity color]
  (let [[x y] (:left-bottom entity)]
    (c/rectangle c x y (:width entity) (:height entity) color)))

(defn- render-entity! [c system entity]
  (try
   (when show-body-bounds
     (draw-body-rect c entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity c) entity)
   (catch Throwable t
     (draw-body-rect c entity :red)
     (pretty-pst t))))

(defn- render-entities [{:keys [cdq.context/player-eid] :as c}]
  (let [entities (map deref (active-entities c))
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [render-below
                    render-default
                    render-above
                    render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (render-entity! c system entity))))

(def ^:private ^:dbg-flag pausing? true)

(defn- check-player-input [{:keys [cdq.context/player-eid] :as c}]
  (state/manual-tick (entity/state-obj @player-eid)
                     c))

(def close-windows-key :escape)

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn- game-loop [{:keys [gdl.context/world-viewport
                          cdq.context/tiled-map
                          cdq.context/player-eid
                          cdq.context/raycaster
                          cdq.context/explored-tile-corners]
                   :as c}]
  (clear-screen black)
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter/create raycaster
                                              explored-tile-corners
                                              (cam/position (:camera world-viewport))))
  (c/draw-on-world-view c
                        (fn [c]
                          (debug/render-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c)
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
    (check-camera-controls c (:camera world-viewport))
    (check-ui-key-listeners c
                            {:controls/close-windows-key close-windows-key
                             :controls/window-hotkeys    window-hotkeys}
                            (c/stage c))
    c))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (app/start (:app     config)
               (:context config)
               game-loop)))

; TODO 'info' missing ?

(def entity
  {:optional [#'gdl.info/info
              #'entity/create
              #'world/create!
              #'destroy
              #'tick
              #'render-below
              #'render-default
              #'render-above
              #'render-info]})

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
             :optional [#'gdl.info/info
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
