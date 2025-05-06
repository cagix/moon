(ns cdq.application
  (:require [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.db :as db]
            [cdq.editor :as editor]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.graphics.animation :as animation]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.info :as info :refer [info-segment]]
            [cdq.input :as input]
            [cdq.inventory :as inventory]
            [cdq.line-of-sight :as los]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [cdq.math.vector2 :as v]
            [cdq.schema :as schema]
            [cdq.skill :as skill]
            [cdq.timer :as timer]
            [cdq.tx :as tx]
            cdq.potential-fields
            [cdq.operation :as op]
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.stage :as stage]
            [cdq.ui.menu :as ui.menu]
            [cdq.utils :as utils :refer [defcomponent safe-merge find-first tile->middle readable-number
                                         pretty-pst sort-by-order]]
            [cdq.val-max :as val-max]
            [cdq.widgets.inventory :as widgets.inventory :refer [remove-item
                                                                 set-item
                                                                 stack-item]]
            [cdq.world :as world :refer [minimum-size
                                         nearest-enemy
                                         friendlies-in-radius
                                         delayed-alert
                                         spawn-audiovisual
                                         spawn-item
                                         item-place-position
                                         world-item?
                                         render-z-order]]
            [cdq.world.potential-field :as potential-field]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.math :as math]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.graphics Color Colors)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport)))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (.setUserObject actor (ui/button-group {:max-check-count 1
                                            :min-check-count 0}))
    actor))

(defn- action-bar* []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (.addActor group (action-bar-button-group))
    group))

(defn- action-bar []
  (ui/table {:rows [[{:actor (action-bar*)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text []
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid world/mouseover-eid]
                (info/text [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid world/mouseover-eid]
    (info/text ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui-actor {:act (fn []
                                        (.setText label (str (->label-text)))
                                        (.pack window))}))
    window))

(defn- render-infostr-on-bar [infostr x y h]
  (graphics/draw-text {:text infostr
                       :x (+ x 75)
                       :y (+ y 2)
                       :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/->sprite "images/rahmen.png")
        hpcontent   (graphics/->sprite "images/hp.png")
        manacontent (graphics/->sprite "images/mana.png")
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (graphics/draw-image rahmen [x y])
                            (graphics/draw-image (graphics/sub-sprite contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message []
  (when-let [text (:text @stage/player-message)]
    (graphics/draw-text {:x (/ (:width     graphics/ui-viewport) 2)
                         :y (+ (/ (:height graphics/ui-viewport) 2) 200)
                         :text text
                         :scale 2.5
                         :up? true})))

(defn- check-remove-message []
  (when (:text @stage/player-message)
    (swap! stage/player-message update :counter + (.getDeltaTime Gdx/graphics))
    (when (>= (:counter @stage/player-message)
              (:duration-seconds @stage/player-message))
      (swap! stage/player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @world/player-eid))}))

(Colors/put "PRETTY_NAME" (Color. (float 0.84) (float 0.8) (float 0.52) (float 1)))

(defmulti ^:private op-value-text (fn [[k]]
                                    k))

(defmethod op-value-text :op/inc
  [[_ value]]
  (str value))

(defmethod op-value-text :op/mult
  [[_ value]]
  (str value "%"))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn- op-info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text component) " " (str/capitalize (name k)))))
             (sort-by op/-order op))))

(defmethod info-segment :property/pretty-name [[_ v] _entity] v)

(defmethod info-segment :maxrange [[_ v] _entity] v)

(defmethod info-segment :creature/level [[_ v] _entity] (str "Level: " v))

(defmethod info-segment :projectile/piercing?  [_ _entity] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key [[_ v] _entity]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time [[_ v] _entity]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown [[_ v] _entity]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info-segment :skill/cost [[_ v] _entity]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info-segment ::stat [[k _] entity]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod info-segment :effects/spawn [[_ {:keys [property/pretty-name]}] _entity]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert [_ _entity]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage [[_ damage] _entity]
  (damage-info damage)
  #_(if source
      (let [modified (entity/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp [[k ops] _entity]
  (op-info ops k))

(defmethod info-segment :effects.target/kill [_ _entity]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage [_ _entity]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb [_ _entity]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun [[_ duration] _entity]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all [_ _entity]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration [[_ counter] _entity]
  (str "Remaining: " (readable-number (timer/ratio counter)) "/1"))

(defmethod info-segment :entity/faction [[_ faction] _entity]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm [[_ fsm] _entity]
  (str "State: " (name (:state fsm))))

(defmethod info-segment :entity/hp [_ entity]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defmethod info-segment :entity/mana [_ entity]
  (str "Mana: " (entity/mana entity)))

(defmethod info-segment :entity/modifiers [[_ mods] _entity]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species [[_ species] _entity]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier [[_ {:keys [counter]}] _entity]
  (str "Spiderweb - remaining: " (readable-number (timer/ratio counter)) "/1"))

#_(defmethod info-segment :entity/skills [skills]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmulti ^:private on-clicked
  (fn [eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid]
  (let [item (:entity/item @eid)]
    (cond
     (Actor/.isVisible (stage/get-inventory))
     (do
      (sound/play! "bfxr_takeit")
      (tx/mark-destroyed eid)
      (tx/event world/player-eid :pickup-item item))

     (inventory/can-pickup-item? (:entity/inventory @world/player-eid) item)
     (do
      (sound/play! "bfxr_pickup")
      (tx/mark-destroyed eid)
      (widgets.inventory/pickup-item world/player-eid item))

     :else
     (do
      (sound/play! "bfxr_denied")
      (stage/show-player-msg! "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_]
  (tx/toggle-inventory-window))

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (fn []
                                              (on-clicked clicked-eid))]
    [(clickable->cursor @clicked-eid true)  (fn []
                                              (sound/play! "bfxr_denied")
                                              (stage/show-player-msg! "Too far away"))]))

(defn- inventory-cell-with-item? [^Actor actor]
  (and (.getParent actor)
       (= "inventory-cell" (.getName (.getParent actor)))
       (get-in (:entity/inventory @world/player-eid)
               (.getUserObject (.getParent actor)))))

(defn- mouseover-actor->cursor []
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)      :cursors/move-window
     (ui/button? actor)                :cursors/over-button
     :else                             :cursors/default)))

(defn- player-effect-ctx [eid]
  (let [target-position (or (and world/mouseover-eid
                                 (:position @world/mouseover-eid))
                            (graphics/world-mouse-position))]
    {:effect/source eid
     :effect/target world/mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and world/mouseover-eid
          (:entity/clickable @world/mouseover-eid))
     (clickable-entity-interaction entity world/mouseover-eid)

     :else
     (if-let [skill-id (stage/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (tx/event eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (sound/play! "bfxr_denied")
               (stage/show-player-msg! (case state
                                         :cooldown "Skill is still on cooldown"
                                         :not-enough-mana "Not enough mana"
                                         :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (sound/play! "bfxr_denied")
          (stage/show-player-msg! "No selected skill"))]))))

(defmethod entity/manual-tick :player-idle [[_ {:keys [eid]}]]
  (if-let [movement-vector (input/player-movement-vector)]
    (tx/event eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state eid)]
      (graphics/set-cursor! cursor)
      (when (input/button-just-pressed? :left)
        (on-click)))))

(defmethod entity/manual-tick :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (and (input/button-just-pressed? :left)
             (world-item?))
    (tx/event eid :drop-item)))

(declare dev-menu-config)

(defn- reset-game! [world-fn]
  (stage/init-state!)
  (Stage/.clear ui/stage)
  (run! stage/add-actor [(ui.menu/create (dev-menu-config))
                         (action-bar)
                         (hp-mana-bar [(/ (:width graphics/ui-viewport) 2)
                                       80 ; action-bar-icon-size
                                       ])
                         (ui/group {:id :windows
                                    :actors [(entity-info-window [(:width graphics/ui-viewport) 0])
                                             (cdq.widgets.inventory/create [(:width  graphics/ui-viewport)
                                                                            (:height graphics/ui-viewport)])]})
                         (player-state-actor)
                         (player-message-actor)])
  (timer/init!)
  (world/create! ((requiring-resolve world-fn) (db/build-all :properties/creatures))))

(declare paused?)

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys @#'db/-schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  (let [window (ui/window {:title "Edit"
                                                           :modal? true
                                                           :close-button? true
                                                           :center? true
                                                           :close-on-escape? true})]
                                    (.add window ^Actor (editor/overview-table property-type editor/edit-property))
                                    (.pack window)
                                    (stage/add-actor window)))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and world/mouseover-eid @world/mouseover-eid)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number timer/elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn (fn [] paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera graphics/world-viewport)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [] (.getFramesPerSecond Gdx/graphics))
                    :icon "images/fps.png"}]})

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration]]
    (timer/create duration))

  (entity/tick! [[_ counter] eid]
    (when (timer/stopped? counter)
      (tx/mark-destroyed eid))))

(defmethod entity/create :entity/hp [[_ v]]
  [v v])

(defmethod entity/create :entity/mana [[_ v]]
  [v v])

(defmethod entity/create :entity/projectile-collision [[_ v]]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod entity/create :active-skill [[_ eid [skill effect-ctx]]]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 timer/create)})

(defmethod entity/create :npc-dead [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-idle [[_ eid]]
  {:eid eid})

(defmethod entity/create :npc-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping [[_ eid]]
  {:eid eid})

(defmethod entity/create :player-idle [[_ eid]]
  {:eid eid})

(defmethod entity/create :player-item-on-cursor [[_ eid item]]
  {:eid eid
   :item item})

(defmethod entity/create :player-moving [[_ eid movement-vector]]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned [[_ eid duration]]
  {:eid eid
   :counter (timer/create duration)})

(defmethod entity/create! :entity/inventory [[k items] eid]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item eid item)))

(defmethod entity/create! :entity/skills [[k skills] eid]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (tx/add-skill eid skill)))

(defmethod entity/create! :entity/animation [[_ animation] eid]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod entity/create! :entity/delete-after-animation-stopped? [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(defmethod entity/create! :entity/fsm [[k {:keys [fsm initial-state]}] eid]
  (swap! eid assoc
         ; fsm throws when initial-state is not part of states, so no need to assert initial-state
         ; initial state is nil, so associng it. make bug report at reduce-fsm?
         k (assoc ((case fsm
                     :fsms/player player-fsm
                     :fsms/npc npc-fsm) initial-state nil) :state initial-state)
         initial-state (entity/create [initial-state eid])))

(defmethod entity/draw-gui-view :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (not (world-item?))
    (graphics/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                            (graphics/mouse-position))))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (los/exists? @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod entity/tick! :active-skill [[_ {:keys [skill effect-ctx counter]}] eid]
  (cond
   (not (effect/some-applicable? (update-effect-ctx effect-ctx)
                                 (:skill/effects skill)))
   (do
    (tx/event eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter)
   (do
    (effect/do-all! effect-ctx (:skill/effects skill))
    (tx/event eid :action-done))))

(defn- npc-choose-skill [entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [eid]
  (let [entity @eid
        target (nearest-enemy entity)
        target (when (and target
                          (los/exists? entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod entity/tick! :npc-idle [_ eid]
  (let [effect-ctx (npc-effect-context eid)]
    (if-let [skill (npc-choose-skill @eid effect-ctx)]
      (tx/event eid :start-action [skill effect-ctx])
      (tx/event eid :movement-direction (or (potential-field/find-direction world/grid eid) [0 0])))))

(defmethod entity/tick! :npc-moving [[_ {:keys [counter]}] eid]
  (when (timer/stopped? counter)
    (tx/event eid :timer-finished)))

(defmethod entity/tick! :npc-sleeping [_ eid]
  (let [entity @eid
        cell (world/grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (tx/event eid :alert)))))

(defmethod entity/tick! :player-moving [[_ {:keys [movement-vector]}] eid]
  (if-let [movement-vector (input/player-movement-vector)]
    (tx/set-movement eid movement-vector)
    (tx/event eid :no-movement-input)))

(defmethod entity/tick! :stunned [[_ {:keys [counter]}] eid]
  (when (timer/stopped? counter)
    (tx/event eid :effect-wears-off)))

(defmethod entity/tick! :entity/alert-friendlies-after-duration [[_ {:keys [counter faction]}] eid]
  (when (timer/stopped? counter)
    (tx/mark-destroyed eid)
    (doseq [friendly-eid (friendlies-in-radius world/grid (:position @eid) faction)]
      (tx/event friendly-eid :alert))))

(defmethod entity/tick! :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation world/delta-time)))))

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (-> body
      (update :position    move-position movement)
      (update :left-bottom move-position movement)))

(defn- valid-position? [grid {:keys [entity/id z-order] :as body}]
  {:pre [(:collides? body)]}
  (let [cells* (into [] (map deref) (grid/rectangle->cells grid body))]
    (and (not-any? #(grid/blocked? % z-order) cells*)
         (->> cells*
              grid/cells->entities
              (not-any? (fn [other-entity]
                          (let [other-entity @other-entity]
                            (and (not= (:entity/id other-entity) id)
                                 (:collides? other-entity)
                                 (entity/collides? other-entity body)))))))))

(defn- try-move [grid body movement]
  (let [new-body (move-body body movement)]
    (when (valid-position? grid new-body)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body movement)
        (try-move grid body (assoc movement :direction [xdir 0]))
        (try-move grid body (assoc movement :direction [0 ydir])))))

; set max speed so small entities are not skipped by projectiles
; could set faster than max-speed if I just do multiple smaller movement steps in one frame
(def ^:private max-speed (/ minimum-size max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def ^:private speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod entity/tick! :entity/movement [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
                                          eid]
  (assert (schema/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time world/delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body world/grid body movement)
                        (move-body body movement))]
        (world/position-changed! eid)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod entity/tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}] eid]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells world/grid entity)) ; just use cached-touched -cells
        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                           (:entity/faction @%))
                                     (:collides? @%)
                                     (entity/collides? entity @%))
                               (grid/cells->entities cells*))
        destroy? (or (and hit-entity (not piercing?))
                     (some #(grid/blocked? % (:z-order entity)) cells*))]
    (when destroy?
      (tx/mark-destroyed eid))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect/do-all! {:effect/source eid
                       :effect/target hit-entity}
                      entity-effects))))

(defmethod entity/tick! :entity/delete-after-animation-stopped? [_ eid]
  (when (animation/stopped? (:entity/animation @eid))
    (tx/mark-destroyed eid)))

(defmethod entity/tick! :entity/skills [[k skills] eid]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? cooling-down?))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod entity/tick! :entity/string-effect [[k {:keys [counter]}] eid]
  (when (timer/stopped? counter)
    (swap! eid dissoc k)))

(defmethod entity/tick! :entity/temp-modifier [[k {:keys [modifiers counter]}] eid]
  (when (timer/stopped? counter)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defcomponent :active-skill
  (state/cursor [_] :cursors/sandclock)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid skill]}]]
    (sound/play! (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :player-idle
  (state/pause-game? [_] true))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)
  (state/pause-game? [_] true)
  (state/enter! [_]
    (sound/play! "bfxr_playerdeath")
    (tx/show-modal {:title "YOU DIED - again!"
                    :text "Good luck next time!"
                    :button-text "OK"
                    :on-click (fn [])})))

(defcomponent :player-item-on-cursor
  (state/cursor [_] :cursors/hand-grab)
  (state/pause-game? [_] true)
  (state/enter! [[_ {:keys [eid item]}]]
    (swap! eid assoc :entity/item-on-cursor item))
  (state/exit! [[_ {:keys [eid]}]]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (sound/play! "bfxr_itemputground")
        (swap! eid dissoc :entity/item-on-cursor)
        (spawn-item (item-place-position entity)
                    (:entity/item-on-cursor entity))))))

(defcomponent :player-moving
  (state/cursor [_] :cursors/walking)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (tx/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :stunned
  (state/cursor [_] :cursors/denied)
  (state/pause-game? [_] false))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid]
    (spawn-audiovisual (:position @eid)
                       (db/build audiovisuals-id))))

(defcomponent :npc-dead
  (state/enter! [[_ {:keys [eid]}]]
    (tx/mark-destroyed eid)))

(defcomponent :npc-moving
  (state/enter! [[_ {:keys [eid movement-vector]}]]
    (tx/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}]]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (state/exit! [[_ {:keys [eid]}]]
    (delayed-alert (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (tx/text-effect eid "[WHITE]!")))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item eid cell item-on-cursor)
      (tx/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item eid cell item-on-cursor)
      (tx/event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play! "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item eid cell)
      (set-item eid cell item-on-cursor)
      (tx/event eid :dropped-item)
      (tx/event eid :pickup-item item-in-cell)))))

(defmethod entity/clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid]}] cell]
  (clicked-cell eid cell))

(defmethod entity/clicked-inventory-cell :player-idle
  [[_ {:keys [eid]}] cell]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (sound/play! "bfxr_takeit")
    (tx/event eid :pickup-item item)
    (remove-item eid cell)))

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [cam (:camera graphics/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/grid (int left-x) (int bottom-y)
                     (inc (int (:width  graphics/world-viewport)))
                     (+ 2 (int (:height graphics/world-viewport)))
                     1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (world/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test []
  (let [position (graphics/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells world/grid circle))]
      (graphics/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position))
          cell (world/grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test)
  (highlight-mouseover-tile))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (graphics/filled-circle center radius [1 1 1 0.125])
    (graphics/sector center
                     radius
                     90 ; start-angle
                     (* (float action-counter-ratio) 360) ; degree
                     [1 1 1 0.5])
    (graphics/draw-image image [(- (float x) radius) y])))

(def ^:private hpbar-colors
  {:green     [0 0.8 0]
   :darkgreen [0 0.5 0]
   :yellow    [0.5 0.5 0]
   :red       [0.5 0 0]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [{:keys [position width half-width half-height]} ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (graphics/pixels->world-units 5)
          border (graphics/pixels->world-units borders-px)]
      (graphics/filled-rectangle x y width height :black)
      (graphics/filled-rectangle (+ x border)
                                 (+ y border)
                                 (- (* width ratio) (* 2 border))
                                 (- height          (* 2 border))
                                 (hpbar-color ratio)))))

(defn- draw-text-when-mouseover-and-text [{:keys [text]}
                                          {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (graphics/draw-text {:text text
                           :x x
                           :y (+ y (:half-height entity))
                           :up? true}))))

(defn- draw-hpbar-when-mouseover-and-not-full [_ entity]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))

(defn- draw-image-as-of-body [image entity]
  (graphics/draw-rotated-centered image
                                  (or (:rotation-angle entity) 0)
                                  (:position entity)))

(defn- draw-line [{:keys [thick? end color]}
                  entity]
  (let [position (:position entity)]
    (if thick?
      (graphics/with-line-width 4 #(graphics/line position end color))
      (graphics/line position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defn- draw-faction-ellipse [_ {:keys [entity/faction] :as entity}]
  (let [player @world/player-eid]
    (graphics/with-line-width 3
      #(graphics/ellipse (:position entity)
                         (:half-width entity)
                         (:half-height entity)
                         (cond (= faction (entity/enemy player))
                               enemy-color
                               (= faction (:entity/faction player))
                               friendly-color
                               :else
                               neutral-color)))))

(defn- render-active-effect [effect-ctx effect]
  (run! #(effect/render % effect-ctx) effect))

(defn- draw-skill-image-and-active-effect [{:keys [skill effect-ctx counter]}
                                           entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image
                      entity
                      (:position entity)
                      (timer/ratio counter))
    (render-active-effect effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defn- draw-zzzz [_ entity]
  (let [[x y] (:position entity)]
    (graphics/draw-text {:text "zzz"
                         :x x
                         :y (+ y (:half-height entity))
                         :up? true})))

(defn- draw-world-item-if-exists [{:keys [item]} entity]
  (when (world-item?)
    (graphics/draw-centered (:entity/image item)
                            (item-place-position entity))))

(defn- draw-stunned-circle [_ entity]
  (graphics/circle (:position entity) 0.5 [1 1 1 0.6]))

(defn- draw-text [{:keys [text]} entity]
  (let [[x y] (:position entity)]
    (graphics/draw-text {:text text
                         :x x
                         :y (+ y
                               (:half-height entity)
                               (graphics/pixels->world-units 5))
                         :scale 2
                         :up? true})))

; TODO draw opacity as of counter ratio?
(defn- draw-filled-circle-grey [_ entity]
  (graphics/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/rectangle x y (:width entity) (:height entity) color)))

(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

(defn- render-entities! []
  (let [entities (map deref world/active-entities)
        player @world/player-eid
        {:keys [below
                default
                above
                info]} entity-render-fns]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [below
                    default
                    above
                    info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (los/exists? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [[k v] entity
               :let [f (get system k)]
               :when f]
         (f v entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (let [player @world/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities world/grid (graphics/world-mouse-position)))]
                    (->> cdq.world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? player @%))
                         first)))]
    (when world/mouseover-eid
      (swap! world/mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (.bindRoot #'world/mouseover-eid new-eid)))

(def pausing? true)

(defn- set-paused-flag! []
  (.bindRoot #'paused? (or #_error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @world/player-eid))
                                (not (or (input/key-just-pressed? :p)
                                         (input/key-pressed?      :space)))))))

(defn- update-time! []
  (let [delta-ms (min (.getDeltaTime Gdx/graphics) max-delta)]
    (timer/inc-state! delta-ms)
    (.bindRoot #'world/delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick world/potential-field-cache
                               world/grid
                               faction
                               world/active-entities
                               max-iterations)))

(defn- tick-entities! []
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid world/active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (stage/error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! []
  (let [camera (:camera graphics/world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! []
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (:windows ui/stage) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (:windows ui/stage))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (db/create!)
    (lwjgl/application! (:application config)
                        (proxy [ApplicationAdapter] []
                          (create []
                            (assets/create! (:assets config))
                            (graphics/create! (:graphics config))
                            (ui/load! (:vis-ui config)
                                      graphics/batch ; we have to pass batch as we use our draw-image/shapes with our other batch inside stage actors
                                      ; -> tests ?, otherwise could use custom batch also from stage itself and not depend on 'graphics', also pass ui-viewport and dont put in graphics
                                      graphics/ui-viewport) ; TODO we don't do dispose! ....
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (assets/dispose!)
                            (graphics/dispose!)
                            ; TODO dispose world/tiled-map !! also @ reset-game ?!
                            )

                          (render []
                            (world/cache-active-entities!)
                            (graphics/set-camera-position! (:position @world/player-eid))
                            (ScreenUtils/clear Color/BLACK)
                            (graphics/draw-tiled-map world/tiled-map
                                                     (tile-color-setter world/raycaster
                                                                        world/explored-tile-corners
                                                                        (camera/position (:camera graphics/world-viewport))))
                            (graphics/draw-on-world-view! (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (Stage/.draw ui/stage)
                            (Stage/.act ui/stage)
                            (entity/manual-tick (entity/state-obj @world/player-eid))
                            (update-mouseover-entity!)
                            (set-paused-flag!)
                            (when-not paused?
                              (update-time!)
                              (update-potential-fields!)
                              (tick-entities!))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (world/remove-destroyed-entities!)

                            (camera-controls!)
                            (window-controls!))

                          (resize [width height]
                            (Viewport/.update graphics/ui-viewport    width height true)
                            (Viewport/.update graphics/world-viewport width height false))))))
