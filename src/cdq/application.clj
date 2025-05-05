(ns cdq.application
  (:require [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.data.grid2d :as g2d]
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
            [cdq.level :as level]
            cdq.level.uf-caves
            cdq.level.modules
            [cdq.math.raycaster :as raycaster]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [cdq.math.vector2 :as v]
            [cdq.rand :refer [rand-int-between]]
            [cdq.schema :as schema]
            [cdq.skill :as skill]
            cdq.time
            [cdq.timer :as timer]
            [cdq.tiled :as tiled]
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
                                         spawn-creature
                                         spawn-item
                                         line-render
                                         spawn-projectile
                                         projectile-size
                                         item-place-position
                                         world-item?
                                         render-z-order
                                         item-place-position]]
            [cdq.world.potential-field :as potential-field]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.math :as math]
            [reduce-fsm :as fsm])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color Colors)
           (com.badlogic.gdx.scenes.scene2d Actor Group Stage)
           (com.badlogic.gdx.utils Disposable ScreenUtils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defmethod level/generate-level* :world.generator/tiled-map [world]
  {:tiled-map (tiled/load-map (:world/tiled-map world))
   :start-position [32 71]})

(defmethod level/generate-level* :world.generator/uf-caves [world]
  (cdq.level.uf-caves/create world
                             (db/build-all :properties/creatures)
                             (assets/get "maps/uf_terrain.png")))

(defmethod level/generate-level* :world.generator/modules [world]
  (cdq.level.modules/generate-modules world
                                      (db/build-all :properties/creatures)))

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

(defn- ->label-text [{:keys [cdq.context/mouseover-eid] :as c}]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid mouseover-eid]
                (info/text c [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid mouseover-eid]
    (info/text c ; don't use select-keys as it loses Entity record type
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
    (.addActor window (ui-actor {:act (fn [context]
                                        (.setText label (str (->label-text context)))
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
    (ui-actor {:draw (fn [{:keys [cdq.context/player-eid]}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message [{:keys [cdq.context/player-message]}]
  (when-let [text (:text @player-message)]
    (graphics/draw-text {:x (/ (:width     graphics/ui-viewport) 2)
                         :y (+ (/ (:height graphics/ui-viewport) 2) 200)
                         :text text
                         :scale 2.5
                         :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message]}]
  (when (:text @player-message)
    (swap! player-message update :counter + (.getDeltaTime Gdx/graphics))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))

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

(defmethod info-segment :property/pretty-name
  [[_ v] _entity _c]
  v)

(defmethod info-segment :maxrange
  [[_ v] _entity _c]
  v)

(defmethod info-segment :creature/level
  [[_ v] _entity _c]
  (str "Level: " v))

(defmethod info-segment :projectile/piercing?
  [_ _entity _c] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key
  [[_ v] _entity _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time
  [[_ v] _entity _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown
  [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info-segment :skill/cost
  [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info-segment ::stat
  [[k _] entity _c]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod info-segment :effects/spawn
  [[_ {:keys [property/pretty-name]}] _entity _context]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert
  [_ _entity _c]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage
  [[_ damage] _entity _c]
  (damage-info damage)
  #_(if source
      (let [modified (entity/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp
  [[k ops] _entity _context]
  (op-info ops k))

(defmethod info-segment :effects.target/kill
  [_ _entity _c]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage
  [_ _entity _c]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb
  [_ _entity _c]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun
  [[_ duration] _entity _c]
  (str "Stuns for " (readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all
  [_ _entity _c]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration
  [[_ counter]
   _entity
   {:keys [cdq.context/elapsed-time] :as c}]
  (str "Remaining: " (readable-number (timer/ratio counter elapsed-time)) "/1"))

(defmethod info-segment :entity/faction
  [[_ faction] _entity _c]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm
  [[_ fsm] _entity _c]
  (str "State: " (name (:state fsm))))

(defmethod info-segment :entity/hp
  [_ entity _c]
  (str "Hitpoints: " (entity/hitpoints entity)))

(defmethod info-segment :entity/mana
  [_ entity _c]
  (str "Mana: " (entity/mana entity)))

(defmethod info-segment :entity/modifiers
  [[_ mods] _entity _c]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species
  [[_ species] _entity _c]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier
  [[_ {:keys [counter]}]
   _entity
   {:keys [cdq.context/elapsed-time] :as c}]
  (str "Spiderweb - remaining: " (readable-number (timer/ratio counter elapsed-time)) "/1"))

#_(defmethod info-segment :entity/skills
    [skills _c]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid
                                       {:keys [cdq.context/player-eid]
                                        :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (Actor/.isVisible (stage/get-inventory))
     (do
      (tx/sound "bfxr_takeit")
      (tx/mark-destroyed eid)
      (tx/event c player-eid :pickup-item item))

     (inventory/can-pickup-item? (:entity/inventory @player-eid) item)
     (do
      (tx/sound "bfxr_pickup")
      (tx/mark-destroyed eid)
      (widgets.inventory/pickup-item player-eid item))

     :else
     (do
      (tx/sound "bfxr_denied")
      (tx/show-player-msg c "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_ c]
  (tx/toggle-inventory-window c))

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [c player-entity clicked-eid]
  (if (< (v/distance (:position player-entity)
                     (:position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (fn []
                                              (on-clicked clicked-eid c))]
    [(clickable->cursor @clicked-eid true)  (fn []
                                              (tx/sound "bfxr_denied")
                                              (tx/show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} ^Actor actor]
  (and (.getParent actor)
       (= "inventory-cell" (.getName (.getParent actor)))
       (get-in (:entity/inventory @player-eid)
               (.getUserObject (.getParent actor)))))

(defn- mouseover-actor->cursor [c]
  (let [actor (stage/mouse-on-actor?)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)        :cursors/move-window
     (ui/button? actor)                  :cursors/over-button
     :else                               :cursors/default)))

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid]} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (graphics/world-mouse-position))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defn- interaction-state [{:keys [cdq.context/mouseover-eid] :as c} eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor?)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (stage/selected-skill)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx c eid)
             state (skill/usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (tx/event c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (tx/sound "bfxr_denied")
               (tx/show-player-msg c (case state
                                       :cooldown "Skill is still on cooldown"
                                       :not-enough-mana "Not enough mana"
                                       :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (tx/sound "bfxr_denied")
          (tx/show-player-msg c "No selected skill"))]))))

(defmethod entity/manual-tick :player-idle [[_ {:keys [eid]}] c]
  (if-let [movement-vector (input/player-movement-vector)]
    (tx/event c eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state c eid)]
      (graphics/set-cursor! cursor)
      (when (input/button-just-pressed? :left)
        (on-click)))))

(defmethod entity/manual-tick :player-item-on-cursor [[_ {:keys [eid]}] c]
  (when (and (input/button-just-pressed? :left)
             (world-item?))
    (tx/event c eid :drop-item)))

(comment
 (ns cdq.components.effects.audiovisual)

 (defn applicable? [_ {:keys [effect/target-position]}]
   target-position)

 (defn useful? [_ _ _c]
   false)

 (defn handle [[_ audiovisual] {:keys [effect/target-position]} c]
   (spawn-audiovisual c target-position audiovisual))
 )

(defcomponent :effects/audiovisual
  (effect/applicable? [_ {:keys [effect/target-position]}]
    target-position)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target-position]} c]
    (spawn-audiovisual c target-position audiovisual)))

(defn- projectile-start-point [entity direction size]
  (v/add (:position entity)
         (v/scale direction
                  (+ (:radius entity) size 0.1))))

(defcomponent :effects/projectile
  ; TODO for npcs need target -- anyway only with direction
  (effect/applicable? [_ {:keys [effect/target-direction]}]
    target-direction) ; faction @ source also ?

  ; TODO valid params direction has to be  non-nil (entities not los player ) ?
  (effect/useful? [[_ {:keys [projectile/max-range] :as projectile}]
                   {:keys [effect/source effect/target]}
                   {:keys [cdq.context/raycaster]}]
    (let [source-p (:position @source)
          target-p (:position @target)]
      ; is path blocked ereally needed? we need LOS also right to have a target-direction as AI?
      (and (not (raycaster/path-blocked? raycaster ; TODO test
                                         source-p
                                         target-p
                                         (projectile-size projectile)))
           ; TODO not taking into account body sizes
           (< (v/distance source-p ; entity/distance function protocol EntityPosition
                          target-p)
              max-range))))

  (effect/handle [[_ projectile] {:keys [effect/source effect/target-direction]} c]
    (spawn-projectile c
                      {:position (projectile-start-point @source
                                                         target-direction
                                                         (projectile-size projectile))
                       :direction target-direction
                       :faction (:entity/faction @source)}
                      projectile)))

(comment
 ; mass shooting
 (for [direction (map math.vector/normalise
                      [[1 0]
                       [1 1]
                       [1 -1]
                       [0 1]
                       [0 -1]
                       [-1 -1]
                       [-1 1]
                       [-1 0]])]
   [:tx/projectile projectile-id ...]
   )
 )

(defcomponent :effects/sound
  (effect/applicable? [_ _ctx]
    true)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ sound] _ctx c]
    (sound/play sound)))

(defcomponent :effects/spawn
  (effect/applicable? [_ {:keys [effect/source effect/target-position]}]
    (and (:entity/faction @source)
         target-position))

  (effect/handle [[_ {:keys [property/id]}]
                  {:keys [effect/source effect/target-position]}
                  c]
    (spawn-creature c
                    {:position target-position
                     :creature-id id ; already properties/get called through one-to-one, now called again.
                     :components {:entity/fsm {:fsm :fsms/npc
                                               :initial-state :npc-idle}
                                  :entity/faction (:entity/faction @source)}})))

(comment
 ; TODO applicable targets? e.g. projectiles/effect s/???item entiteis ??? check
 ; same code as in render entities on world view screens/world
 ; TODO showing one a bit further up
 ; maybe world view port is cut
 ; not quite showing correctly.
 (let [targets (los/creatures-in-los-of-player)]
   (count targets)
   #_(sort-by #(% 1) (map #(vector (:entity.creature/name @%)
                                   (:position @%)) targets)))

 )

(defcomponent :effects/target-all
  ; TODO targets projectiles with -50% hp !!
  (effect/applicable? [_ _]
    true)

  (effect/useful? [_ _ _c]
    ; TODO
    false)

  (effect/handle [[_ {:keys [entity-effects]}] {:keys [effect/source]} c]
    (let [source* @source]
      (doseq [target (los/creatures-in-los-of-player c)]
        (line-render c
                     {:start (:position source*) #_(start-point source* target*)
                      :end (:position @target)
                      :duration 0.05
                      :color [1 0 0 0.75]
                      :thick? true})
        ; some sound .... or repeat smae sound???
        ; skill do sound  / skill start sound >?
        ; problem : nested tx/effect , we are still having direction/target-position
        ; at sub-effects
        ; and no more safe - merge
        ; find a way to pass ctx / effect-ctx separate ?
        (tx/effect c
                   {:effect/source source :effect/target target}
                   entity-effects))))

  (effect/render [_ {:keys [effect/source]} c]
    (let [source* @source]
      (doseq [target* (map deref (los/creatures-in-los-of-player c))]
        (graphics/line (:position source*) #_(start-point source* target*)
                           (:position target*)
                           [1 0 0 0.5])))))

(defcomponent :effects/target-entity
  (effect/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
    (and target
         (seq (effect/filter-applicable? effect-ctx entity-effects))))

  (effect/useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
    (entity/in-range? @source @target maxrange))

  (effect/handle [[_ {:keys [maxrange entity-effects]}]
                  {:keys [effect/source effect/target] :as effect-ctx}
                  c]
    (let [source* @source
          target* @target]
      (if (entity/in-range? source* target* maxrange)
        (do
         (line-render c
                      {:start (entity/start-point source* target*)
                       :end (:position target*)
                       :duration 0.05
                       :color [1 0 0 0.75]
                       :thick? true})
         (tx/effect c effect-ctx entity-effects))
        (spawn-audiovisual c
                           (entity/end-point source* target* maxrange)
                           (db/build :audiovisuals/hit-ground)))))

  (effect/render [[_ {:keys [maxrange]}]
                  {:keys [effect/source effect/target]}
                  _context]
    (when target
      (let [source* @source
            target* @target]
        (graphics/line (entity/start-point source* target*)
                           (entity/end-point source* target* maxrange)
                           (if (entity/in-range? source* target* maxrange)
                             [1 0 0 0.5]
                             [1 1 0 0.5]))))))

(defcomponent :effects.target/audiovisual
  (effect/applicable? [_ {:keys [effect/target]}]
    target)

  (effect/useful? [_ _ _c]
    false)

  (effect/handle [[_ audiovisual] {:keys [effect/target]} c]
    (spawn-audiovisual c
                       (:position @target)
                       audiovisual)))

(defcomponent :effects.target/convert
  (effect/applicable? [_ {:keys [effect/source effect/target]}]
    (and target
         (= (:entity/faction @target)
            (entity/enemy @source))))

  (effect/handle [_ {:keys [effect/source effect/target]} c]
    (swap! target assoc :entity/faction (:entity/faction @source))))

(defn- effective-armor-save [source* target*]
  (max (- (or (entity/stat target* :entity/armor-save) 0)
          (or (entity/stat source* :entity/armor-pierce) 0))
       0))

(comment
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )


(defn- armor-saves? [source* target*]
  (< (rand) (effective-armor-save source* target*)))

(defcomponent :effects.target/damage
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/hp @target)))

  (effect/handle [[_ damage]
                  {:keys [effect/source effect/target]}
                  c]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (tx/text-effect c target "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (spawn-audiovisual c
                            (:position target*)
                            (db/build :audiovisuals/damage))
         (tx/event c target (if (zero? new-hp-val) :kill :alert))
         (tx/text-effect c target (str "[RED]" dmg-amount "[]")))))))

(defcomponent :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]} c]
    (tx/event c target :kill)))

(defn- entity->melee-damage [entity]
  (let [strength (or (entity/stat entity :entity/strength) 0)]
    {:damage/min-max [strength strength]}))

(defn- melee-damage-effect [entity]
  [:effects.target/damage (entity->melee-damage entity)])

(defcomponent :effects.target/melee-damage
  (effect/applicable? [_ {:keys [effect/source] :as ctx}]
    (effect/applicable? (melee-damage-effect @source) ctx))

  (effect/handle [_ {:keys [effect/source] :as ctx} c]
    (effect/handle (melee-damage-effect @source) ctx c)))

(let [modifiers {:modifier/movement-speed {:op/mult -0.5}}
      duration 5]
  (defcomponent :effects.target/spiderweb
    (effect/applicable? [_ _]
      ; ?
      true)

    ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
    (effect/handle [_
                    {:keys [effect/target]}
                    {:keys [cdq.context/elapsed-time] :as c}]
      (when-not (:entity/temp-modifier @target)
        (swap! target assoc :entity/temp-modifier {:modifiers modifiers
                                                   :counter (timer/create elapsed-time duration)})
        (swap! target entity/mod-add modifiers)))))

(defcomponent :effects.target/stun
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [[_ duration] {:keys [effect/target]} c]
    (tx/event c target :stun duration)))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied))

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defn- player-entity-props [start-position]
  {:position (utils/tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-enemies! [{:keys [cdq.context/tiled-map] :as c}]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position utils/tile->middle)))
  :ok)

(defn- spawn-creatures! [{:keys [cdq.context/level
                                 cdq.context/tiled-map]
                          :as context}]
  (spawn-enemies! context)
  (spawn-creature context
                  (player-entity-props (:start-position level))))

(declare dev-menu-config)

(defn- create-stage-actors []
  [(ui.menu/create (dev-menu-config))
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

(defn- reset-stage! []
  (Stage/.clear ui/stage)
  (run! stage/add-actor (create-stage-actors)))

(defn- reset-game! [{:keys [world-id]}]
  (reset-stage!)
  (let [{:keys [tiled-map start-position] :as level} (level/create world-id)
        grid (create-grid tiled-map)
        _ (world/create! tiled-map)
        context {:cdq.context/elapsed-time 0
                 :cdq.context/entity-ids (atom {})
                 :cdq.context/player-message (atom {:duration-seconds 1.5})
                 :cdq.context/level level
                 :cdq.context/error nil
                 :cdq.context/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                           (tiled/tm-height tiled-map)
                                                                           (constantly false)))
                 :cdq.context/grid grid
                 :cdq.context/tiled-map tiled-map
                 :cdq.context/raycaster (raycaster grid)
                 :cdq.context/factions-iterations {:good 15 :evil 5}
                 :world/potential-field-cache (atom nil)}]
    (assoc context :cdq.context/player-eid (spawn-creatures! context))))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration] {:keys [cdq.context/elapsed-time]}]
    (timer/create elapsed-time duration))

  (entity/tick! [[_ counter] eid {:keys [cdq.context/elapsed-time]}]
    (when (timer/stopped? counter elapsed-time)
      (tx/mark-destroyed eid))))

(defmethod entity/create :entity/hp
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/mana
  [[_ v] _c]
  [v v])

(defmethod entity/create :entity/projectile-collision
  [[_ v] c]
  (assoc v :already-hit-bodies #{}))

(defn- apply-action-speed-modifier [entity skill action-time]
  (/ action-time
     (or (entity/stat entity (:skill/action-time-modifier-key skill))
         1)))

(defmethod entity/create :active-skill
  [[_ eid [skill effect-ctx]]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :skill skill
   :effect-ctx effect-ctx
   :counter (->> skill
                 :skill/action-time
                 (apply-action-speed-modifier @eid skill)
                 (timer/create elapsed-time))})

(defmethod entity/create :npc-dead
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-idle
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :npc-moving
  [[_ eid movement-vector]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :movement-vector movement-vector
   :counter (timer/create elapsed-time (* (entity/stat @eid :entity/reaction-time) 0.016))})

(defmethod entity/create :npc-sleeping
  [[_ eid] c]
  {:eid eid})

(defmethod entity/create :player-dead
  [[k] _c]
  (db/build :player-dead/component.enter))

(defmethod entity/create :player-idle
  [[_ eid] _c]
  (safe-merge (db/build :player-idle/clicked-inventory-cell)
              {:eid eid}))

(defmethod entity/create :player-item-on-cursor
  [[_ eid item] _c]
  (safe-merge (db/build :player-item-on-cursor/component)
              {:eid eid
               :item item}))

(defmethod entity/create :player-moving
  [[_ eid movement-vector] c]
  {:eid eid
   :movement-vector movement-vector})

(defmethod entity/create :stunned
  [[_ eid duration]
   {:keys [cdq.context/elapsed-time]}]
  {:eid eid
   :counter (timer/create elapsed-time duration)})

(defmethod entity/create! :entity/inventory
  [[k items] eid _c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item eid item)))

(defmethod entity/create! :entity/skills
  [[k skills] eid _c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (tx/add-skill eid skill)))

(defmethod entity/create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod entity/create! :entity/delete-after-animation-stopped?
  [_ eid c]
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

(defmethod entity/create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         ; fsm throws when initial-state is not part of states, so no need to assert initial-state
         ; initial state is nil, so associng it. make bug report at reduce-fsm?
         k (assoc ((case fsm
                     :fsms/player player-fsm
                     :fsms/npc npc-fsm) initial-state nil) :state initial-state)
         initial-state (entity/create [initial-state eid] c)))

(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] _c]
  (when (not (world-item?))
    (graphics/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                            (graphics/mouse-position))))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (los/exists? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod entity/tick! :active-skill [[_ {:keys [skill effect-ctx counter]}]
                                      eid
                                      {:keys [cdq.context/elapsed-time] :as c}]
  (cond
   (not (effect/some-applicable? (update-effect-ctx c effect-ctx)
                                 (:skill/effects skill)))
   (do
    (tx/event c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter elapsed-time)
   (do
    (tx/effect c effect-ctx (:skill/effects skill))
    (tx/event c eid :action-done))))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % ctx))
                     (effect/applicable-and-useful? c ctx (:skill/effects %))))
       first))

(defn- npc-effect-context [c eid]
  (let [entity @eid
        target (nearest-enemy c entity)
        target (when (and target
                          (los/exists? c entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (entity/direction entity @target))}))

(defmethod entity/tick! :npc-idle [_ eid c]
  (let [effect-ctx (npc-effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (tx/event c eid :start-action [skill effect-ctx])
      (tx/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))

(defmethod entity/tick! :npc-moving [[_ {:keys [counter]}]
                                    eid
                                    {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/event c eid :timer-finished)))

(defmethod entity/tick! :npc-sleeping [_ eid {:keys [cdq.context/grid] :as c}]
  (let [entity @eid
        cell (grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (tx/event c eid :alert)))))

(defmethod entity/tick! :player-moving [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (input/player-movement-vector)]
    (tx/set-movement eid movement-vector)
    (tx/event c eid :no-movement-input)))

(defmethod entity/tick! :stunned [[_ {:keys [counter]}]
                                 eid
                                 {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/event c eid :effect-wears-off)))

(defmethod entity/tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}]
   eid
   {:keys [cdq.context/grid
           cdq.context/elapsed-time]
    :as c}]
  (when (timer/stopped? counter elapsed-time)
    (tx/mark-destroyed eid)
    (doseq [friendly-eid (friendlies-in-radius grid (:position @eid) faction)]
      (tx/event c friendly-eid :alert))))

(defmethod entity/tick! :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

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
(def ^:private max-speed (/ minimum-size
                            cdq.time/max-delta)) ; need to make var because s/schema would fail later if divide / is inside the schema-form

(def ^:private speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod entity/tick! :entity/movement
  [[_ {:keys [direction speed rotate-in-movement-direction?] :as movement}]
   eid
   {:keys [cdq.context/delta-time
           cdq.context/grid] :as context}]
  (assert (schema/validate speed-schema speed)
          (pr-str speed))
  (assert (or (zero? (v/length direction))
              (v/normalised? direction))
          (str "cannot understand direction: " (pr-str direction)))
  (when-not (or (zero? (v/length direction))
                (nil? speed)
                (zero? speed))
    (let [movement (assoc movement :delta-time delta-time)
          body @eid]
      (when-let [body (if (:collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                        (try-move-solid-body grid body movement)
                        (move-body body movement))]
        (world/position-changed! eid context)
        (swap! eid assoc
               :position (:position body)
               :left-bottom (:left-bottom body))
        (when rotate-in-movement-direction?
          (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))))))

(defmethod entity/tick! :entity/projectile-collision
  [[k {:keys [entity-effects already-hit-bodies piercing?]}]
   eid
   {:keys [cdq.context/grid] :as c}]
  ; TODO this could be called from body on collision
  ; for non-solid
  ; means non colliding with other entities
  ; but still collding with other stuff here ? o.o
  (let [entity @eid
        cells* (map deref (grid/rectangle->cells grid entity)) ; just use cached-touched -cells
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
      (tx/effect c
                 {:effect/source eid
                  :effect/target hit-entity}
                 entity-effects))))

(defmethod entity/tick! :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (tx/mark-destroyed eid)))

(defmethod entity/tick! :entity/skills
  [[k skills]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? cooling-down? elapsed-time))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod entity/tick! :entity/string-effect
  [[k {:keys [counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)))

(defmethod entity/tick! :entity/temp-modifier
  [[k {:keys [modifiers counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

(defcomponent :active-skill
  (state/cursor [_] :cursors/sandclock)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid skill]}]
                 {:keys [cdq.context/elapsed-time] :as c}]
    (sound/play (:skill/start-action-sound skill))
    (when (:skill/cooldown skill)
      (swap! eid assoc-in
             [:entity/skills (:property/id skill) :skill/cooling-down?]
             (timer/create elapsed-time (:skill/cooldown skill))))
    (when (and (:skill/cost skill)
               (not (zero? (:skill/cost skill))))
      (swap! eid entity/pay-mana-cost (:skill/cost skill)))))

(defcomponent :player-idle
  (state/pause-game? [_] true))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)
  (state/pause-game? [_] true)
  (state/enter! [[_ {:keys [tx/sound
                            modal/title
                            modal/text
                            modal/button-text]}]
                 _c]
    (sound/play sound)
    (tx/show-modal {:title title
                    :text text
                    :button-text button-text
                    :on-click (fn [])})))

(defcomponent :player-item-on-cursor
  (state/cursor [_] :cursors/hand-grab)
  (state/pause-game? [_] true)
  (state/enter! [[_ {:keys [eid item]}] c]
    (swap! eid assoc :entity/item-on-cursor item))
  (state/exit! [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
    ; at clicked-cell when we put it into a inventory-cell
    ; we do not want to drop it on the ground too additonally,
    ; so we dissoc it there manually. Otherwise it creates another item
    ; on the ground
    (let [entity @eid]
      (when (:entity/item-on-cursor entity)
        (sound/play place-world-item-sound)
        (swap! eid dissoc :entity/item-on-cursor)
        (spawn-item c
                    (item-place-position c entity)
                    (:entity/item-on-cursor entity))))))

(defcomponent :player-moving
  (state/cursor [_] :cursors/walking)
  (state/pause-game? [_] false)
  (state/enter! [[_ {:keys [eid movement-vector]}] c]
    (tx/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :stunned
  (state/cursor [_] :cursors/denied)
  (state/pause-game? [_] false))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid c]
    (spawn-audiovisual c
                       (:position @eid)
                       (db/build audiovisuals-id))))

(defcomponent :npc-dead
  (state/enter! [[_ {:keys [eid]}] c]
    (tx/mark-destroyed eid)))

(defcomponent :npc-moving
  (state/enter! [[_ {:keys [eid movement-vector]}] c]
    (tx/set-movement eid movement-vector))
  (state/exit! [[_ {:keys [eid]}] c]
    (swap! eid dissoc :entity/movement)))

(defcomponent :npc-sleeping
  (state/exit! [[_ {:keys [eid]}] c]
    (delayed-alert c
                   (:position       @eid)
                   (:entity/faction @eid)
                   0.2)
    (tx/text-effect c eid "[WHITE]!")))

(defn- clicked-cell [{:keys [player-item-on-cursor/item-put-sound]} eid cell c]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item eid cell item-on-cursor)
      (tx/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item c eid cell item-on-cursor)
      (tx/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item eid cell)
      (set-item eid cell item-on-cursor)
      (tx/event c eid :dropped-item)
      (tx/event c eid :pickup-item item-in-cell)))))

(defmethod entity/clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid] :as data}] cell c]
  (clicked-cell data eid cell c))

(defmethod entity/clicked-inventory-cell :player-idle
  [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (sound/play pickup-item-sound)
    (tx/event c eid :pickup-item item)
    (remove-item eid cell)))

(defn- player-state-input! [{:keys [cdq.context/player-eid]
                             :as context}]
  (entity/manual-tick (entity/state-obj @player-eid) context)
  context)

(defn- assoc-active-entities [context]
  (assoc context :cdq.game/active-entities (world/get-active-entities context)))

(defn- set-camera-on-player! [{:keys [cdq.context/player-eid]
                               :as context}]
  (graphics/set-camera-position! (:position @player-eid))
  context)

(defn- clear-screen! [context]
  (ScreenUtils/clear Color/BLACK)
  context)

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

(defn- render-tiled-map! [{:keys [cdq.context/tiled-map
                                  cdq.context/raycaster
                                  cdq.context/explored-tile-corners]
                           :as context}]
  (graphics/draw-tiled-map tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera graphics/world-viewport))))
  context)

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- draw-before-entities! [{:keys [cdq.context/factions-iterations
                                      cdq.context/grid]}]
  (let [cam (:camera graphics/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/grid (int left-x) (int bottom-y)
                     (inc (int (:width  graphics/world-viewport)))
                     (+ 2 (int (:height graphics/world-viewport)))
                     1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (grid [x y])]
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

(defn- geom-test [{:keys [cdq.context/grid]}]
  (let [position (graphics/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
      (graphics/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [{:keys [cdq.context/grid]}]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position))
          cell (grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5]))))))

(defn- draw-after-entities! [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))

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

(defn- draw-text-when-mouseover-and-text
  [{:keys [text]}
   {:keys [entity/mouseover?] :as entity}
   _context]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (graphics/draw-text {:text text
                           :x x
                           :y (+ y (:half-height entity))
                           :up? true}))))

(defn- draw-hpbar-when-mouseover-and-not-full [_ entity _c]
  (let [ratio (val-max/ratio (entity/hitpoints entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))

(defn- draw-image-as-of-body [image entity _context]
  (graphics/draw-rotated-centered image
                                  (or (:rotation-angle entity) 0)
                                  (:position entity)))

(defn- draw-line
  [{:keys [thick? end color]}
   entity
   _context]
  (let [position (:position entity)]
    (if thick?
      (graphics/with-line-width 4 #(graphics/line position end color))
      (graphics/line position end color))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defn- draw-faction-ellipse
  [_
   {:keys [entity/faction] :as entity}
   {:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid]
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

(defn- render-active-effect [context effect-ctx effect]
  (run! #(effect/render % effect-ctx context) effect))

(defn- draw-skill-image-and-active-effect
  [{:keys [skill effect-ctx counter]}
   entity
   {:keys [cdq.context/elapsed-time] :as c}]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image
                      entity
                      (:position entity)
                      (timer/ratio counter elapsed-time))
    (render-active-effect c
                          effect-ctx
                          ; !! FIXME !!
                          ; (update-effect-ctx c effect-ctx)
                          ; - render does not need to update .. update inside active-skill
                          effects)))

(defn- draw-zzzz [_ entity _context]
  (let [[x y] (:position entity)]
    (graphics/draw-text {:text "zzz"
                         :x x
                         :y (+ y (:half-height entity))
                         :up? true})))

(defn- draw-world-item-if-exists [{:keys [item]} entity c]
  (when (world-item?)
    (graphics/draw-centered (:entity/image item)
                            (item-place-position c entity))))

(defn- draw-stunned-circle [_ entity _context]
  (graphics/circle (:position entity) 0.5 [1 1 1 0.6]))

(defn- draw-text [{:keys [text]} entity _context]
  (let [[x y] (:position entity)]
    (graphics/draw-text {:text text
                         :x x
                         :y (+ y
                               (:half-height entity)
                               (graphics/pixels->world-units 5))
                         :scale 2
                         :up? true})))

; TODO draw opacity as of counter ratio?
(defn- draw-filled-circle-grey [_ entity _context]
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

(defn- render-entities!
  [{:keys [cdq.context/player-eid
           cdq.game/active-entities] :as c}]
  (let [entities (map deref active-entities)
        player @player-eid
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
                      (los/exists? c player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [[k v] entity
               :let [f (get system k)]
               :when f]
         (f v entity c))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- draw-on-world-view! [context]
  (graphics/draw-on-world-view! (fn []
                                  (doseq [f [draw-before-entities!
                                             render-entities!
                                             draw-after-entities!]]
                                    (f context))))
  context)

(defn- stage-draw! [context]
  (set! (.applicationState ui/stage) context)
  (Stage/.draw ui/stage)
  context)

(defn- stage-act! [context]
  (set! (.applicationState ui/stage) context)
  (Stage/.act ui/stage)
  context)

(defn- update-mouseover-entity! [{:keys [cdq.context/grid
                                         cdq.context/mouseover-eid
                                         cdq.context/player-eid]
                                  :as context}]
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (graphics/world-mouse-position)))]
                    (->> cdq.world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? context player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc context :cdq.context/mouseover-eid new-eid)))

(defn- set-paused-flag [{:keys [cdq.context/player-eid
                                error ; FIXME ! not `::` keys so broken !
                                ]
                         :as context}]
  (let [pausing? true]
    (assoc context :cdq.context/paused? (or error
                                            (and pausing?
                                                 (state/pause-game? (entity/state-obj @player-eid))
                                                 (not (or (input/key-just-pressed? :p)
                                                          (input/key-pressed?      :space))))))))

(defn- update-time [context]
  (let [delta-ms (min (.getDeltaTime Gdx/graphics)
                      cdq.time/max-delta)]
    (-> context
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn- update-potential-fields! [{:keys [cdq.context/factions-iterations
                                         cdq.context/grid
                                         world/potential-field-cache
                                         cdq.game/active-entities]
                                  :as context}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick potential-field-cache
                               grid
                               faction
                               active-entities
                               max-iterations))
  context)

(defn- tick-entities! [{:keys [cdq.game/active-entities]
                        :as context}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid context))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (stage/error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  context)

(defn- when-not-paused! [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f]
              (f context))
            context
            [update-time
             update-potential-fields!
             tick-entities!])))

(defn- camera-controls! [context]
  (let [camera (:camera graphics/world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn- window-controls! [context]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (:windows ui/stage) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (:windows ui/stage))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows))))
  context)

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (db/create!)
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource (:dock-icon (:mac-os config)))))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (assets/create! (:assets config))
                            (graphics/create! (:graphics config))
                            (ui/load! (:vis-ui config)
                                      graphics/batch ; we have to pass batch as we use our draw-image/shapes with our other batch inside stage actors
     ; -> tests ?, otherwise could use custom batch also from stage itself and not depend on 'graphics', also pass ui-viewport and dont put in graphics
                                      graphics/ui-viewport) ; TODO we don't do dispose! ....
                            (reset! state (reset-game! config)))

                          (dispose []
                            (assets/dispose!)
                            (graphics/dispose!)
                            (doseq [[k obj] @state]
                              (if (instance? Disposable obj)
                                (do
                                 #_(println "Disposing:" k)
                                 (Disposable/.dispose obj))
                                #_(println "Not Disposable: " k ))))

                          (render []
                            (swap! state (fn [context]
                                           (reduce (fn [context f]
                                                     (f context))
                                                   context
                                                   [assoc-active-entities
                                                    set-camera-on-player!
                                                    clear-screen!
                                                    render-tiled-map!
                                                    draw-on-world-view!
                                                    stage-draw!
                                                    stage-act!
                                                    player-state-input!
                                                    update-mouseover-entity!
                                                    set-paused-flag
                                                    when-not-paused!

                                                    ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                                                    world/remove-destroyed-entities!

                                                    camera-controls!
                                                    window-controls!]))))

                          (resize [width height]
                            (Viewport/.update graphics/ui-viewport    width height true)
                            (Viewport/.update graphics/world-viewport width height false)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle (:title config))
                          (.setWindowedMode (:width  (:windowed-mode config))
                                            (:height (:windowed-mode config)))
                          (.setForegroundFPS (:foreground-fps config))))))

(defn post-runnable!
  "`f` should be a `(fn [context])`.

  Is executed after the main-loop, in order not to interfere with it."
  [f]
  (.postRunnable Gdx/app (fn [] (f @state))))

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world (map db/build [:worlds/vampire
                                              :worlds/modules
                                              :worlds/uf-caves])]
                     {:label (str "Start " (:property/id world))
                      :on-click (fn []
                                  (reset! state (reset-game! {:world-id (:property/id world)})))})}
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
                    :update-fn (fn [{:keys [cdq.context/mouseover-eid]}]
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [cdq.context/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn :cdq.context/paused?} ; TODO (def paused ::paused) @ cdq.context
                   {:label "GUI"
                    :update-fn (fn [_] (graphics/mouse-position))}
                   {:label "World"
                    :update-fn (fn [_] (mapv int (graphics/world-mouse-position)))}
                   {:label "Zoom"
                    :update-fn (fn [_] (camera/zoom (:camera graphics/world-viewport)))
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn (fn [_]
                                 (.getFramesPerSecond Gdx/graphics))
                    :icon "images/fps.png"}]})
