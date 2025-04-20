(ns cdq.application
  (:require [cdq.assets :refer [play-sound]]
            [cdq.context :as context]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.effect-context :as effect-ctx]
            [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.schema :as schema]
            [cdq.property :as property]
            [cdq.timer :as timer]
            cdq.error
            [cdq.grid :as grid]
            [cdq.line-of-sight :as los]
            cdq.potential-fields
            cdq.time
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :refer [nearest-enemy
                               player-movement-vector
                               friendlies-in-radius
                               minimum-size
                               item-place-position
                               show-modal
                               delayed-alert
                               spawn-item
                               spawn-audiovisual
                               spawn-creature
                               spawn-projectile
                               line-render
                               projectile-size
                               world-item?
                               player-movement-vector
                               get-inventory
                               show-player-msg
                               selected-skill]]
            cdq.world.context
            [cdq.world.potential-field :as potential-field]
            [gdl.assets :as assets]
            [gdl.audio.sound :as sound]
            [gdl.data.grid2d :as g2d]
            [gdl.gdx.interop :as interop]
            [gdl.graphics :as graphics]
            [gdl.graphics.animation :as animation]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            gdl.graphics.sprite
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            [gdl.input :as input]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.vector2 :as v]
            [gdl.rand :refer [rand-int-between]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdl.ui.stage :as stage]
            [gdl.utils :as utils :refer [find-first defcomponent]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color Pixmap Pixmap$Format Texture Texture$TextureFilter OrthographicCamera)
           (com.badlogic.gdx.graphics.g2d Batch BitmapFont SpriteBatch TextureRegion)
           (com.badlogic.gdx.graphics.g2d.freetype FreeTypeFontGenerator
                                                   FreeTypeFontGenerator$FreeTypeFontParameter)
           (com.badlogic.gdx.math MathUtils)
           (com.badlogic.gdx.utils SharedLibraryLoader Os)
           (com.badlogic.gdx.utils.viewport Viewport FitViewport)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (gdl StageWithState OrthogonalTiledMapRenderer)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)
           (space.earlygrey.shapedrawer ShapeDrawer)))

(defmulti manual-tick (fn [[k] context]
                        k))
(defmethod manual-tick :default [_ c])

(defn- player-state-input [{:keys [cdq.context/player-eid] :as c}]
  (manual-tick (entity/state-obj @player-eid) c)
  c)

(defn- player-effect-ctx [{:keys [cdq.context/mouseover-eid
                                  gdl.graphics/world-viewport]} eid]
  (let [target-position (or (and mouseover-eid
                                 (:position @mouseover-eid))
                            (gdl.graphics/world-mouse-position world-viewport))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:position @eid) target-position)}))

(defmulti ^:private on-clicked
  (fn [eid c]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [eid {:keys [cdq.context/player-eid] :as c}]
  (let [item (:entity/item @eid)]
    (cond
     (actor/visible? (get-inventory c))
     (do
      (play-sound c "bfxr_takeit")
      (swap! eid assoc :entity/destroyed? true)
      (fsm/event c player-eid :pickup-item item))

     (entity/can-pickup-item? @player-eid item)
     (do
      (play-sound c "bfxr_pickup")
      (swap! eid assoc :entity/destroyed? true)
      (widgets.inventory/pickup-item c player-eid item))

     :else
     (do
      (play-sound c "bfxr_denied")
      (show-player-msg c "Your Inventory is full")))))

(defmethod on-clicked :clickable/player [_ c]
  (actor/toggle-visible! (get-inventory c)))

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
                                              (play-sound c "bfx_denied")
                                              (show-player-msg c "Too far away"))]))

(defn- inventory-cell-with-item? [{:keys [cdq.context/player-eid] :as c} actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/name (actor/parent actor)))
       (get-in (:entity/inventory @player-eid)
               (actor/user-object (actor/parent actor)))))

(defn- mouseover-actor->cursor [{:keys [cdq.context/stage] :as c}]
  (let [actor (stage/mouse-on-actor? stage)]
    (cond
     (inventory-cell-with-item? c actor) :cursors/hand-before-grab
     (ui/window-title-bar? actor)           :cursors/move-window
     (ui/button? actor)                     :cursors/over-button
     :else                               :cursors/default)))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defn- skill-usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill} effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect-ctx/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn- interaction-state [{:keys [cdq.context/mouseover-eid
                                  cdq.context/stage] :as c} eid]
  (let [entity @eid]
    (cond
     (stage/mouse-on-actor? stage)
     [(mouseover-actor->cursor c)
      (fn [] nil)] ; handled by actors themself, they check player state

     (and mouseover-eid
          (:entity/clickable @mouseover-eid))
     (clickable-entity-interaction c entity mouseover-eid)

     :else
     (if-let [skill-id (selected-skill c)]
       (let [skill (skill-id (:entity/skills entity))
             effect-ctx (player-effect-ctx c eid)
             state (skill-usable-state entity skill effect-ctx)]
         (if (= state :usable)
           (do
            ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
            ; different color ?
            ; => e.g. meditation no TARGET .. etc.
            [:cursors/use-skill
             (fn []
               (fsm/event c eid :start-action [skill effect-ctx]))])
           (do
            ; TODO cursor as of usable state
            ; cooldown -> sanduhr kleine
            ; not-enough-mana x mit kreis?
            ; invalid-params -> depends on params ...
            [:cursors/skill-not-usable
             (fn []
               (play-sound c "bfxr_denied")
               (show-player-msg c (case state
                                    :cooldown "Skill is still on cooldown"
                                    :not-enough-mana "Not enough mana"
                                    :invalid-params "Cannot use this here")))])))
       [:cursors/no-skill-selected
        (fn []
          (play-sound c "bfxr_denied")
          (show-player-msg c "No selected skill"))]))))

(defmethod manual-tick :player-idle [[_ {:keys [eid]}] c]
  (if-let [movement-vector (player-movement-vector)]
    (fsm/event c eid :movement-input movement-vector)
    (let [[cursor on-click] (interaction-state c eid)]
      (gdl.graphics/set-cursor c cursor)
      (when (input/button-just-pressed? :left)
        (on-click)))))

(defmethod manual-tick :player-item-on-cursor [[_ {:keys [eid]}] c]
  (when (and (input/button-just-pressed? :left)
             (world-item? c))
    (fsm/event c eid :drop-item)))

(defmethod schema/edn->value :s/sound [_ sound-name {:keys [cdq/assets]}]
  (cdq.assets/sound assets sound-name))

(defn- edn->sprite [c {:keys [file sub-image-bounds]}]
  (if sub-image-bounds
    (let [[sprite-x sprite-y] (take 2 sub-image-bounds)
          [tilew tileh]       (drop 2 sub-image-bounds)]
      (gdl.graphics.sprite/from-sheet (gdl.graphics.sprite/sheet c file tilew tileh)
                                      [(int (/ sprite-x tilew))
                                       (int (/ sprite-y tileh))]
                                      c))
    (gdl.graphics.sprite/create c file)))

(defmethod schema/edn->value :s/image [_ edn c]
  (edn->sprite c edn))

(defmethod schema/edn->value :s/animation [_ {:keys [frames frame-duration looping?]} c]
  (gdl.graphics.animation/create (map #(edn->sprite c %) frames)
                                     :frame-duration frame-duration
                                     :looping? looping?))

(defmethod schema/edn->value :s/one-to-one [_ property-id {:keys [cdq/db] :as context}]
  (db/build db property-id context))

(defmethod schema/edn->value :s/one-to-many [_ property-ids {:keys [cdq/db] :as context}]
  (set (map #(db/build db % context) property-ids)))

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
        (effect-ctx/do-all! c
                            {:effect/source source :effect/target target}
                            entity-effects)))))

(defcomponent :effects/target-entity
  (effect/applicable? [[_ {:keys [entity-effects]}] {:keys [effect/target] :as effect-ctx}]
    (and target
         (seq (effect-ctx/filter-applicable? effect-ctx entity-effects))))

  (effect/useful?  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]} _c]
    (entity/in-range? @source @target maxrange))

  (effect/handle [[_ {:keys [maxrange entity-effects]}]
                  {:keys [effect/source effect/target] :as effect-ctx}
                  {:keys [cdq/db] :as c}]
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
         (effect-ctx/do-all! c effect-ctx entity-effects))
        (spawn-audiovisual c
                           (entity/end-point source* target* maxrange)
                           (db/build db :audiovisuals/hit-ground c))))))

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
                  {:keys [cdq/db] :as c}]
    (let [source* @source
          target* @target
          hp (entity/hitpoints target*)]
      (cond
       (zero? (hp 0))
       nil

       (armor-saves? source* target*)
       (swap! target entity/add-text-effect c "[WHITE]ARMOR")

       :else
       (let [min-max (:damage/min-max (entity/damage source* target* damage))
             dmg-amount (rand-int-between min-max)
             new-hp-val (max (- (hp 0) dmg-amount) 0)]
         (swap! target assoc-in [:entity/hp 0] new-hp-val)
         (spawn-audiovisual c
                            (:position target*)
                            (db/build db :audiovisuals/damage c))
         (fsm/event c target (if (zero? new-hp-val) :kill :alert))
         (swap! target entity/add-text-effect c (str "[RED]" dmg-amount "[]")))))))

(defcomponent :effects.target/kill
  (effect/applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (effect/handle [_ {:keys [effect/target]} c]
    (fsm/event c target :kill)))

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
    (fsm/event c target :stun duration)))

(defprotocol Disposable
  (dispose! [_]))

(extend-type com.badlogic.gdx.utils.Disposable
  Disposable
  (dispose! [this]
    (.dispose this)))

(defn- load-assets [{:keys [folder
                            asset-type->extensions]}]
  (assets/create
   (for [[asset-type extensions] asset-type->extensions
         file (map #(str/replace-first % folder "")
                   (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
                          result []]
                     (cond (nil? file)
                           result

                           (.isDirectory file)
                           (recur (concat remaining (.list file)) result)

                           (extensions (.extension file))
                           (recur remaining (conj result (.path file)))

                           :else
                           (recur remaining result))))]
     [file asset-type])))

(defrecord Cursors []
  Disposable
  (dispose! [this]
    (run! dispose! (vals this))))

(defn- load-cursors [config]
  (map->Cursors
   (gdl.utils/mapvals
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

(defn- degree->radians [degree]
  (* MathUtils/degreesToRadians (float degree)))

(defn- sd-set-color [shape-drawer color]
  (ShapeDrawer/.setColor shape-drawer (interop/->color color)))

(extend-type ShapeDrawer
  gdl.graphics.shape-drawer/ShapeDrawer
  (ellipse [this [x y] radius-x radius-y color]
    (sd-set-color this color)
    (.ellipse this
              (float x)
              (float y)
              (float radius-x)
              (float radius-y)))

  (filled-ellipse [this [x y] radius-x radius-y color]
    (sd-set-color this color)
    (.filledEllipse this
                    (float x)
                    (float y)
                    (float radius-x)
                    (float radius-y)))

  (circle [this [x y] radius color]
    (sd-set-color this color)
    (.circle this
             (float x)
             (float y)
             (float radius)))

  (filled-circle [this [x y] radius color]
    (sd-set-color this color)
    (.filledCircle this
                   (float x)
                   (float y)
                   (float radius)))

  (arc [this [center-x center-y] radius start-angle degree color]
    (sd-set-color this color)
    (.arc this
          (float center-x)
          (float center-y)
          (float radius)
          (float (degree->radians start-angle))
          (float (degree->radians degree))))

  (sector [this [center-x center-y] radius start-angle degree color]
    (sd-set-color this color)
    (.sector this
             (float center-x)
             (float center-y)
             (float radius)
             (float (degree->radians start-angle))
             (float (degree->radians degree))))

  (rectangle [this x y w h color]
    (sd-set-color this color)
    (.rectangle this
                (float x)
                (float y)
                (float w)
                (float h)))

  (filled-rectangle [this x y w h color]
    (sd-set-color this color)
    (.filledRectangle this
                      (float x)
                      (float y)
                      (float w)
                      (float h)))

  (line [this [sx sy] [ex ey] color]
    (sd-set-color this color)
    (.line this
           (float sx)
           (float sy)
           (float ex)
           (float ey)))

  (with-line-width [this width draw-fn]
    (let [old-line-width (.getDefaultLineWidth this)]
      (.setDefaultLineWidth this (float (* width old-line-width)))
      (draw-fn)
      (.setDefaultLineWidth this (float old-line-width)))))

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

(defn- entity-components []
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
                                    (show-modal c {:title title
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
                                    (swap! eid assoc :entity/movement {:direction movement-vector
                                                                       :speed (entity/stat @eid :entity/movement-speed)}))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :stunned               {:pause-game? false
                           :cursor :cursors/denied}
   :npc-dead              {:enter (fn [[_ {:keys [eid]}] c]
                                    (swap! eid assoc :entity/destroyed? true))}
   :npc-moving            {:enter (fn [[_ {:keys [eid movement-vector]}] c]
                                    (swap! eid assoc :entity/movement {:direction movement-vector
                                                                       :speed (or (entity/stat @eid :entity/movement-speed) 0)}))
                           :exit (fn [[_ {:keys [eid]}] c]
                                   (swap! eid dissoc :entity/movement))}
   :npc-sleeping          {:exit (fn [[_ {:keys [eid]}] c]
                                   (delayed-alert c
                                                  (:position       @eid)
                                                  (:entity/faction @eid)
                                                  0.2)
                                   (swap! eid entity/add-text-effect c "[WHITE]!"))}})

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

(defn- set-camera-on-player
  [{:keys [gdl.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  {:pre [world-viewport
         player-eid]}
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)

(defn- clear-screen! [context]
  (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
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

(defn- render-tiled-map! [{:keys [gdl.graphics/world-viewport
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

(defn- draw-with [{:keys [^Batch gdl.graphics/batch
                          gdl.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera viewport)))
  (.begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :cdq.context/unit-scale unit-scale))))
  (.end batch))

(defn- draw-on-world-view* [{:keys [gdl.graphics/world-unit-scale
                                    gdl.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(defn- draw-on-world-view! [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)

(defn- render-stage! [{:keys [^StageWithState cdq.context/stage] :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (com.badlogic.gdx.scenes.scene2d.Stage/.draw stage)
  (set! (.applicationState stage) context)
  (com.badlogic.gdx.scenes.scene2d.Stage/.act stage)
  context)

(defn- update-mouseover-entity! [{:keys [cdq.context/grid
                                         cdq.context/mouseover-eid
                                         cdq.context/player-eid
                                         gdl.graphics/world-viewport
                                         cdq.context/stage] :as c}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (gdl.graphics/world-mouse-position world-viewport)))]
                    (->> cdq.world/render-z-order
                         (gdl.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn- update-paused! [{:keys [cdq.context/player-eid
                               context/entity-components
                               error ; FIXME ! not `::` keys so broken !
                               ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))

(defn- update-time [context]
  (let [delta-ms (min (graphics/delta-time)
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

(defmulti tick! (fn [[k] eid c]
                  k))
(defmethod tick! :default [_ eid c])

(defn- tick-entities! [{:keys [cdq.game/active-entities] :as context}]
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
               (tick! [k v] eid context))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (cdq.error/error-window context t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  context)

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [context {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (los/exists? context @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defmethod tick! :active-skill [[_ {:keys [skill effect-ctx counter]}]
                                eid
                                {:keys [cdq.context/elapsed-time] :as c}]
  (cond
   (not (effect-ctx/some-applicable? (update-effect-ctx c effect-ctx)
                                     (:skill/effects skill)))
   (do
    (fsm/event c eid :action-done)
    ; TODO some sound ?
    )

   (timer/stopped? counter elapsed-time)
   (do
    (effect-ctx/do-all! c effect-ctx (:skill/effects skill))
    (fsm/event c eid :action-done))))

(defn- npc-choose-skill [c entity ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill-usable-state entity % ctx))
                     (effect-ctx/applicable-and-useful? c ctx (:skill/effects %))))
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

(defmethod tick! :npc-idle [_ eid c]
  (let [effect-ctx (npc-effect-context c eid)]
    (if-let [skill (npc-choose-skill c @eid effect-ctx)]
      (fsm/event c eid :start-action [skill effect-ctx])
      (fsm/event c eid :movement-direction (or (potential-field/find-direction c eid) [0 0])))))

(defmethod tick! :npc-moving [[_ {:keys [counter]}]
                              eid
                              {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :timer-finished)))

(defmethod tick! :npc-sleeping [_ eid {:keys [cdq.context/grid] :as c}]
  (let [entity @eid
        cell (grid (entity/tile entity))]
    (when-let [distance (grid/nearest-entity-distance @cell (entity/enemy entity))]
      (when (<= distance (entity/stat entity :entity/aggro-range))
        (fsm/event c eid :alert)))))

(defmethod tick! :player-moving [[_ {:keys [movement-vector]}] eid c]
  (if-let [movement-vector (player-movement-vector)]
    (swap! eid assoc :entity/movement {:direction movement-vector
                                       :speed (entity/stat @eid :entity/movement-speed)})
    (fsm/event c eid :no-movement-input)))

(defmethod tick! :stunned [[_ {:keys [counter]}]
                           eid
                           {:keys [cdq.context/elapsed-time] :as c}]
  (when (timer/stopped? counter elapsed-time)
    (fsm/event c eid :effect-wears-off)))

(defmethod tick! :entity/alert-friendlies-after-duration
  [[_ {:keys [counter faction]}]
   eid
   {:keys [cdq.context/grid
           cdq.context/elapsed-time]
    :as c}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)
    (doseq [friendly-eid (friendlies-in-radius grid (:position @eid) faction)]
      (fsm/event c friendly-eid :alert))))

(defmethod tick! :entity/animation
  [[k animation] eid {:keys [cdq.context/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc k (animation/tick animation delta-time)))))

(defmethod tick! :entity/delete-after-duration
  [[_ counter]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid assoc :entity/destroyed? true)))

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

(def speed-schema (schema/m-schema [:and number? [:>= 0] [:<= max-speed]]))

(defmethod tick! :entity/movement
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

(defmethod tick! :entity/projectile-collision
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
      (swap! eid assoc :entity/destroyed? true))
    (when hit-entity
      (swap! eid assoc-in [k :already-hit-bodies] (conj already-hit-bodies hit-entity))) ; this is only necessary in case of not piercing ...
    (when hit-entity
      (effect-ctx/do-all! c
                          {:effect/source eid
                           :effect/target hit-entity}
                          entity-effects))))

(defmethod tick! :entity/delete-after-animation-stopped?
  [_ eid c]
  (when (animation/stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))

(defmethod tick! :entity/skills
  [[k skills]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (doseq [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (timer/stopped? cooling-down? elapsed-time))]
    (swap! eid assoc-in [k (:property/id skill) :skill/cooling-down?] false)))

(defmethod tick! :entity/string-effect
  [[k {:keys [counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)))

(defmethod tick! :entity/temp-modifier
  [[k {:keys [modifiers counter]}]
   eid
   {:keys [cdq.context/elapsed-time]}]
  (when (timer/stopped? counter elapsed-time)
    (swap! eid dissoc k)
    (swap! eid entity/mod-remove modifiers)))

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

(defn- camera-controls! [{:keys [gdl.graphics/world-viewport]
                          :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn- window-controls! [c]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows (:cdq.context/stage c)) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows (:cdq.context/stage c)))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  c)

(def state (atom nil))

(defn- application-listener [config]
  (proxy [ApplicationAdapter] []
    (create []
      (reset! state (let [batch (SpriteBatch.)
                          shape-drawer-texture (white-pixel-texture)
                          world-unit-scale (float (/ (:world-unit-scale config)))
                          ; TODO ui-viewport part of stage?
                          ui-viewport (fit-viewport (:width  (:ui-viewport config))
                                                    (:height (:ui-viewport config))
                                                    (OrthographicCamera.))
                          schemas (-> (:schemas config) io/resource slurp edn/read-string)
                          context {:cdq/assets (load-assets (:assets config))
                                   :gdl.graphics/batch batch
                                   :gdl.graphics/cursors (load-cursors (:cursors config))
                                   :gdl.graphics/default-font (load-font (:default-font config))
                                   :gdl.graphics/shape-drawer (ShapeDrawer. batch (TextureRegion. ^Texture shape-drawer-texture 1 0 1 1))
                                   :gdl.graphics/shape-drawer-texture shape-drawer-texture
                                   :gdl.graphics/tiled-map-renderer (tiled-map-renderer batch world-unit-scale)
                                   :gdl.graphics/ui-viewport ui-viewport
                                   :gdl.graphics/world-unit-scale world-unit-scale
                                   :gdl.graphics/world-viewport (world-viewport world-unit-scale (:world-viewport config))
                                   :cdq.context/stage (create-stage! (:ui config) batch ui-viewport)
                                   :cdq/schemas schemas
                                   :cdq/db (create-db schemas)
                                   :context/entity-components (entity-components)}]
                      (cdq.world.context/reset context (:first-level config)))))

    (dispose []
      (doseq [[k value] @state]
        (if (satisfies? Disposable value)
          (do
           #_(println "Disposing:" k)
           (dispose! value))
          #_(println "Not Disposable: " k ))))

    (render []
      (swap! state (fn [context]
                     (reduce (fn [context f]
                               (f context))
                             context
                             [assoc-active-entities
                              set-camera-on-player
                              clear-screen!
                              render-tiled-map!
                              draw-on-world-view!
                              render-stage!
                              player-state-input
                              update-mouseover-entity!
                              update-paused!
                              when-not-paused!

                              ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                              remove-destroyed-entities!

                              camera-controls!
                              window-controls!]))))

    (resize [width height]
      (let [context @state]
        (Viewport/.update (:gdl.graphics/ui-viewport    context) width height true)
        (Viewport/.update (:gdl.graphics/world-viewport context) width height false)))))

(defn- start! [config]
  (when (= SharedLibraryLoader/os Os/MacOsX)
    (.setIconImage (Taskbar/getTaskbar)
                   (.getImage (Toolkit/getDefaultToolkit)
                              (io/resource (:dock-icon (:mac-os config)))))
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
  (Lwjgl3Application. (:listener config)
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle (:title config))
                        (.setWindowedMode (:width  (:windowed-mode config))
                                          (:height (:windowed-mode config)))
                        (.setForegroundFPS (:foreground-fps config)))))

(defn -main []
  (start! {:mac-os {:dock-icon "moon.png"}
           :title "Cyber Dungeon Quest"
           :windowed-mode {:width 1440 :height 900}
           :foreground-fps 60
           :listener (application-listener
                      {:assets {:folder "resources/"
                                :asset-type->extensions {:sound   #{"wav"}
                                                         :texture #{"png" "bmp"}}}
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
                       :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                                      :size 16
                                      :quality-scaling 2}
                       :ui-viewport {:width 1440 :height 900}
                       :world-unit-scale 48
                       :world-viewport {:width 1440 :height 900}
                       :ui {:skin-scale :x1}
                       :schemas "schema.edn"
                       :first-level :worlds/vampire})}))

(defn post-runnable [f]
  (.postRunnable Gdx/app (fn [] (f @state))))
