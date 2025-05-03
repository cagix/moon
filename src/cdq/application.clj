(ns cdq.application
  (:require [cdq.assets :as assets]
            [cdq.audio.sound :as sound]
            [cdq.context :as context]
            [cdq.data.grid2d :as g2d]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.gdx.interop :as interop]
            [cdq.graphics :as graphics]
            [cdq.graphics.animation :as animation]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.shape-drawer :as shape-drawer]
            cdq.graphics.sprite
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.inventory :as inventory]
            [cdq.line-of-sight :as los]
            [cdq.property :as property]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.vector2 :as v]
            [cdq.schema :as schema]
            [cdq.skill :as skill]
            cdq.time
            [cdq.timer :as timer]
            [cdq.tx :as tx]
            cdq.potential-fields
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.utils :as utils :refer [defcomponent safe-merge find-first]]
            [cdq.widgets.inventory :as widgets.inventory :refer [remove-item
                                                                 set-item
                                                                 stack-item]]
            [cdq.world :refer [minimum-size
                               nearest-enemy
                               friendlies-in-radius
                               delayed-alert
                               spawn-audiovisual
                               spawn-item
                               item-place-position
                               world-item?]]
            [cdq.world.potential-field :as potential-field]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]])
  (:import (cdq StageWithState OrthogonalTiledMapRenderer)
           (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils Disposable ScreenUtils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport FitViewport Viewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

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
  [[k] {:keys [cdq/db] :as c}]
  (db/build db :player-dead/component.enter c))

(defmethod entity/create :player-idle
  [[_ eid] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-idle/clicked-inventory-cell c)
              {:eid eid}))

(defmethod entity/create :player-item-on-cursor
  [[_ eid item] {:keys [cdq/db] :as c}]
  (safe-merge (db/build db :player-item-on-cursor/component c)
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
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item c eid item)))

(defmethod entity/create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (tx/add-skill c eid skill)))

(defmethod entity/create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod entity/create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod entity/create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (fsm/create fsm initial-state)
         initial-state (entity/create [initial-state eid] c)))

(defmethod entity/draw-gui-view :player-item-on-cursor
  [[_ {:keys [eid]}] {:keys [cdq.graphics/ui-viewport] :as c}]
  (when (not (world-item? c))
    (graphics/draw-centered c
                            (:entity/image (:entity/item-on-cursor @eid))
                            (graphics/mouse-position ui-viewport))))

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
        (doseq [component context]
          (context/position-changed component eid))
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

(def ^:private entity-components
  {:entity/destroy-audiovisual {:destroy! (fn [audiovisuals-id eid {:keys [cdq/db] :as c}]
                                            (spawn-audiovisual c
                                                               (:position @eid)
                                                               (db/build db audiovisuals-id c)))}
   :player-idle           {:pause-game? true}
   :active-skill          {:pause-game? false
                           :cursor :cursors/sandclock
                           :enter (fn [[_ {:keys [eid skill]}]
                                       {:keys [cdq.context/elapsed-time] :as c}]
                                    (sound/play (:skill/start-action-sound skill))
                                    (when (:skill/cooldown skill)
                                      (swap! eid assoc-in
                                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                                             (timer/create elapsed-time (:skill/cooldown skill))))
                                    (when (and (:skill/cost skill)
                                               (not (zero? (:skill/cost skill))))
                                      (swap! eid entity/pay-mana-cost (:skill/cost skill))))}
   :player-dead           {:pause-game? true
                           :cursor :cursors/black-x
                           :enter (fn [[_ {:keys [tx/sound
                                                  modal/title
                                                  modal/text
                                                  modal/button-text]}]
                                       c]
                                    (sound/play sound)
                                    (tx/show-modal c {:title title
                                                      :text text
                                                      :button-text button-text
                                                      :on-click (fn [])}))}
   :player-item-on-cursor {:pause-game? true
                           :cursor :cursors/hand-grab
                           :enter (fn [[_ {:keys [eid item]}] c]
                                    (swap! eid assoc :entity/item-on-cursor item))
                           :exit (fn [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
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
                                                   (:entity/item-on-cursor entity)))))}
   :player-moving         {:pause-game? false
                           :cursor :cursors/walking
                           :enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (tx/set-movement eid movement-vector))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :stunned               {:pause-game? false
                           :cursor :cursors/denied}
   :npc-dead              {:enter (fn [[_ {:keys [eid]}] c]
                                    (tx/mark-destroyed eid))}
   :npc-moving            {:enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (tx/set-movement eid movement-vector))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :npc-sleeping          {:exit (fn [[_ {:keys [eid]}] c]
                                   (delayed-alert c
                                                  (:position       @eid)
                                                  (:entity/faction @eid)
                                                  0.2)
                                   (tx/text-effect c eid "[WHITE]!"))}})

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
      (set-item c eid cell item-on-cursor)
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
      (remove-item c eid cell)
      (set-item c eid cell item-on-cursor)
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
    (remove-item c eid cell)))

(defn- player-state-input! [{:keys [cdq.context/player-eid]
                             :as context}]
  (entity/manual-tick (entity/state-obj @player-eid) context)
  context)

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn- assoc-active-entities [{:keys [cdq.context/content-grid
                                      cdq.context/player-eid]
                               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))

(defn- set-camera-on-player!
  [{:keys [cdq.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
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

(defn- render-tiled-map! [{:keys [cdq.graphics/world-viewport
                                  cdq.context/tiled-map
                                  cdq.context/raycaster
                                  cdq.context/explored-tile-corners]
                           :as context}]
  (tiled-map-renderer/draw context
                           tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera world-viewport))))
  context)

(def ^:private render-fns
  '[(cdq.render.draw-on-world-view.before-entities/render)
    (cdq.render.draw-on-world-view.entities/render-entities
     {:below {:entity/mouseover? cdq.render.draw-on-world-view.entities/draw-faction-ellipse
              :player-item-on-cursor cdq.render.draw-on-world-view.entities/draw-world-item-if-exists
              :stunned cdq.render.draw-on-world-view.entities/draw-stunned-circle}
      :default {:entity/image cdq.render.draw-on-world-view.entities/draw-image-as-of-body
                :entity/clickable cdq.render.draw-on-world-view.entities/draw-text-when-mouseover-and-text
                :entity/line-render cdq.render.draw-on-world-view.entities/draw-line}
      :above {:npc-sleeping cdq.render.draw-on-world-view.entities/draw-zzzz
              :entity/string-effect cdq.render.draw-on-world-view.entities/draw-text
              :entity/temp-modifier cdq.render.draw-on-world-view.entities/draw-filled-circle-grey}
      :info {:entity/hp cdq.render.draw-on-world-view.entities/draw-hpbar-when-mouseover-and-not-full
             :active-skill cdq.render.draw-on-world-view.entities/draw-skill-image-and-active-effect}})
    (cdq.render.draw-on-world-view.after-entities/render)])

(defn- draw-with! [{:keys [^Batch cdq.graphics/batch
                           cdq.graphics/shape-drawer]
                    :as context}
                   viewport
                   unit-scale
                   draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera viewport)))
  (.begin batch)
  (shape-drawer/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc context :cdq.context/unit-scale unit-scale))))
  (.end batch))

(defn- draw-on-world-view* [{:keys [cdq.graphics/world-unit-scale
                                    cdq.graphics/world-viewport]
                             :as context}
                            render-fn]
  (draw-with! context
              world-viewport
              world-unit-scale
              render-fn))

(defn- draw-on-world-view! [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)

(defn- stage-draw! [{:keys [^StageWithState cdq.context/stage]
                     :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (Stage/.draw stage)
  context)

(defn- stage-act! [{:keys [^StageWithState cdq.context/stage]
                    :as context}]
  (set! (.applicationState stage) context)
  (Stage/.act stage)
  context)

(defn- update-mouseover-entity! [{:keys [cdq.context/grid
                                         cdq.context/mouseover-eid
                                         cdq.context/player-eid
                                         cdq.graphics/world-viewport
                                         cdq.context/stage]
                                  :as context}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (graphics/world-mouse-position world-viewport)))]
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
                                context/entity-components
                                error ; FIXME ! not `::` keys so broken !
                                ]
                         :as context}]
  (let [pausing? true]
    (assoc context :cdq.context/paused? (or error
                                            (and pausing?
                                                 (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
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
     (stage/error-window! (:cdq.context/stage context) t)
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

(defn- remove-destroyed-entities! [{:keys [cdq.context/entity-ids
                                           context/entity-components]
                                    :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (doseq [component context]
      (context/remove-entity component eid))
    (doseq [[k v] @eid
            :let [destroy! (get-in entity-components [k :destroy!])]
            :when destroy!]
      (destroy! v eid context)))
  context)

(defn- camera-controls! [{:keys [cdq.graphics/world-viewport]
                          :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn- window-controls! [{:keys [cdq.context/stage]
                          :as context}]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  context)

(defrecord Cursors []
  Disposable
  (dispose [this]
    (run! Disposable/.dispose (vals this))))

(defn- load-cursors [config]
  (map->Cursors
   (utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
            cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
        (.dispose pixmap)
        cursor))
    config)))

(defn- font-params [{:keys [size]}]
  (let [params (FreeTypeFontGenerator$FreeTypeFontParameter.)]
    (set! (.size params) size)
    ; .color and this:
    ;(set! (.borderWidth parameter) 1)
    ;(set! (.borderColor parameter) red)
    (set! (.minFilter params) Texture$TextureFilter/Linear) ; because scaling to world-units
    (set! (.magFilter params) Texture$TextureFilter/Linear)
    params))

(defn- generate-font [file-handle params]
  (let [generator (FreeTypeFontGenerator. file-handle)
        font (.generateFont generator (font-params params))]
    (.dispose generator)
    font))

(defn- load-font [{:keys [file size quality-scaling]}]
  (let [^BitmapFont font (generate-font (.internal Gdx/files file)
                                        {:size (* size quality-scaling)})]
    (.setScale (.getData font) (float (/ quality-scaling)))
    (set! (.markupEnabled (.getData font)) true)
    (.setUseIntegerPositions font false) ; otherwise scaling to world-units (/ 1 48)px not visible
    font))

(defn- white-pixel-texture []
  (let [pixmap (doto (Pixmap. 1 1 Pixmap$Format/RGBA8888)
                 (.setColor Color/WHITE)
                 (.drawPixel 0 0))
        texture (Texture. pixmap)]
    (.dispose pixmap)
    texture))

(defn- create-stage! [config batch viewport]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case (:skin-scale config)
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (let [stage (proxy [StageWithState ILookup] [viewport batch]
                (valAt
                  ([id]
                   (group/find-actor-with-id (StageWithState/.getRoot this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (StageWithState/.getRoot this) id)
                       not-found))))]
    (.setInputProcessor Gdx/input stage)
    stage))

(defn- tiled-map-renderer [batch world-unit-scale]
  (memoize (fn [tiled-map]
             (OrthogonalTiledMapRenderer. tiled-map
                                          (float world-unit-scale)
                                          batch))))

(defn- fit-viewport
  "A ScalingViewport that uses Scaling.fit so it keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the remaining space."
  [width height camera & {:keys [center-camera?]}]
  {:pre [width height]}
  (proxy [FitViewport ILookup] [width height camera]
    (valAt
      ([key]
       (interop/k->viewport-field this key))
      ([key _not-found]
       (interop/k->viewport-field this key)))))

(defn- world-viewport [world-unit-scale config]
  {:pre [world-unit-scale]}
  (let [camera (OrthographicCamera.)
        world-width  (* (:width  config) world-unit-scale)
        world-height (* (:height config) world-unit-scale)
        y-down? false]
    (.setToOrtho camera y-down? world-width world-height)
    (fit-viewport world-width world-height camera)))

; reduce-kv?
(defn- apply-kvs
  "Calls for every key in map (f k v) to calculate new value at k."
  [m f]
  (reduce (fn [m k]
            (assoc m k (f k (get m k)))) ; using assoc because non-destructive for records
          m
          (keys m)))

(defmethod schema/edn->value :s/sound [_ sound-name {:keys [cdq/assets]}]
  (assets/sound assets sound-name))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (cdq.graphics.sprite/from-sheet (cdq.graphics.sprite/sheet c file tilew tileh)
                                      [(int (/ sprite-x tilew))
                                       (int (/ sprite-y tileh))]
                                      c))
    (cdq.graphics.sprite/create c file)))

(defmethod schema/edn->value :s/image [_ edn c]
  (edn->sprite c edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} c]
  (cdq.graphics.animation/create (map #(edn->sprite c %) frames)
                                     :frame-duration frame-duration
                                     :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id {:keys [cdq/db] :as context}]
  (db/build db property-id context))

(defmethod schema/edn->value :s/one-to-many [_ property-ids {:keys [cdq/db] :as context}]
  (set (map #(db/build db % context) property-ids)))

(defn- validate-properties! [properties schemas]
  (assert (or (empty? properties)
              (apply distinct? (map :property/id properties))))
  (run! #(schema/validate! schemas (property/type %) %) properties))

#_(def ^:private undefined-data-ks (atom #{}))

(comment
 #{:frames
   :looping?
   :frame-duration
   :file ; => this is texture ... convert that key itself only?!
   :sub-image-bounds})

(defn- build* [{:keys [cdq/schemas] :as c} property]
  (apply-kvs property
             (fn [k v]
               (let [schema (try (schema/schema-of schemas k)
                                 (catch Throwable _t
                                   #_(swap! undefined-data-ks conj k)
                                   nil))
                     v (if (map? v)
                         (build* c v)
                         v)]
                 (try (schema/edn->value schema v c)
                      (catch Throwable t
                        (throw (ex-info " " {:k k :v v} t))))))))

(defn- recur-sort-map [m]
  (into (sorted-map)
        (zipmap (keys m)
                (map #(if (map? %)
                        (recur-sort-map %)
                        %)
                     (vals m)))))

(defn- async-pprint-spit! [file data]
  (.start
   (Thread.
    (fn []
      (binding [*print-level* nil]
        (->> data
             pprint
             with-out-str
             (spit file)))))))

(defrecord DB []
  db/DB
  (async-write-to-file! [{:keys [db/data db/properties-file]}]
    ; TODO validate them again!?
    (->> data
         vals
         (sort-by property/type)
         (map recur-sort-map)
         doall
         (async-pprint-spit! properties-file)))

  (update [{:keys [db/data] :as db}
           {:keys [property/id] :as property}
           schemas]
    {:pre [(contains? property :property/id)
           (contains? data id)]}
    (schema/validate! schemas (property/type property) property)
    (clojure.core/update db :db/data assoc id property)) ; assoc-in ?

  (delete [{:keys [db/data] :as db} property-id]
    {:pre [(contains? data property-id)]}
    (clojure.core/update db dissoc :db/data property-id)) ; dissoc-in ?

  (get-raw [{:keys [db/data]} id]
    (utils/safe-get data id))

  (all-raw [{:keys [db/data]} property-type]
    (->> (vals data)
         (filter #(= property-type (property/type %)))))

  (build [this id context]
    (build* context (db/get-raw this id)))

  (build-all [this property-type context]
    (map (partial build* context)
         (db/all-raw this property-type))))

(defn- create-db [schemas]
  (let [properties-file (io/resource "properties.edn")
        properties (-> properties-file slurp edn/read-string)]
    (validate-properties! properties schemas)
    (map->DB {:db/data (zipmap (map :property/id properties) properties)
              :db/properties-file properties-file})))

(defn- create-initial-context! [config]
  (let [batch (SpriteBatch.)
        shape-drawer-texture (white-pixel-texture)
        world-unit-scale (float (/ (:world-unit-scale config)))
        ui-viewport (fit-viewport (:width  (:ui-viewport config))
                                  (:height (:ui-viewport config))
                                  (OrthographicCamera.))
        schemas (-> (:schemas config) io/resource slurp edn/read-string)]
    {:cdq/assets (assets/create (:assets config))
     ;;
     :cdq.graphics/batch batch
     :cdq.graphics/cursors (load-cursors (:cursors config))
     :cdq.graphics/default-font (load-font (:default-font config))
     :cdq.graphics/shape-drawer (shape-drawer/create batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
     :cdq.graphics/shape-drawer-texture shape-drawer-texture
     :cdq.graphics/tiled-map-renderer (tiled-map-renderer batch world-unit-scale)
     :cdq.graphics/world-unit-scale world-unit-scale
     :cdq.graphics/world-viewport (world-viewport world-unit-scale (:world-viewport config))
     ;;
     :cdq.graphics/ui-viewport ui-viewport
     :cdq.context/stage (create-stage! (:ui config) batch ui-viewport)
     ;;
     :cdq/schemas schemas
     :cdq/db (create-db schemas)
     ;;
     }))

(def state
  "Do not call `swap!`, instead use `post-runnable!`, as the main game loop has side-effects and should not be retried.

  (Should probably make this private and have a `get-state` function)"
  (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create-pipeline config))]
    (doseq [ns (:requires config)]
      #_(println "requiring " ns)
      (require ns))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource (:dock-icon (:mac-os config)))))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (reset! state (reduce (fn [context f]
                                                    (f context config))
                                                  (assoc (create-initial-context! config)
                                                         :context/entity-components entity-components)
                                                  create-pipeline)))

                          (dispose []
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
                                                    remove-destroyed-entities!

                                                    camera-controls!
                                                    window-controls!]))))

                          (resize [width height]
                            (let [context @state]
                              (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
                              (Viewport/.update (:cdq.graphics/world-viewport context) width height false))))
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
