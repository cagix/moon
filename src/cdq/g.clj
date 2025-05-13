(ns cdq.g
  (:require [cdq.audio.sound :as sound]
            [cdq.assets :as assets]
            [cdq.db :as db]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.g.db]
            [cdq.g.graphics]
            [cdq.g.world]
            [cdq.graphics :as graphics]
            [cdq.stage]
            [cdq.ui.error-window :as error-window]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            cdq.world.potential-fields
            [clojure.data.animation :as animation]
            [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.input :as input]
            [clojure.gdx.interop :as interop]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.math :refer [circle->outer-rectangle]]
            [clojure.gdx.math.raycaster :as raycaster]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.utils.disposable :refer [dispose!]]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [clojure.java.io :as io]
            [clojure.timer :as timer]
            [clojure.utils :as utils :refer [sort-by-order
                                             define-order
                                             safe-merge
                                             tile->middle
                                             pretty-pst
                                             bind-root]]))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal ctx/stage)))
  (stage/add-actor! ctx/stage
                    (ui/window {:title title
                                :rows [[(ui/label text)]
                                       [(ui/text-button button-text
                                                        (fn []
                                                          (actor/remove! (::modal ctx/stage))
                                                          (on-click)))]]
                                :id ::modal
                                :modal? true
                                :center-position [(/ (:width  (:ui-viewport ctx/graphics)) 2)
                                                  (* (:height (:ui-viewport ctx/graphics)) (/ 3 4))]
                                :pack? true})))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta 0.04)

(defn- set-cells! [grid eid]
  (let [cells (grid/rectangle->cells grid @eid)]
    (assert (not-any? nil? cells))
    (swap! eid assoc ::touched-cells cells)
    (doseq [cell cells]
      (assert (not (get (:entities @cell) eid)))
      (swap! cell update :entities conj eid))))

(defn- remove-from-cells! [eid]
  (doseq [cell (::touched-cells @eid)]
    (assert (get (:entities @cell) eid))
    (swap! cell update :entities disj eid)))

; could use inside tiles only for >1 tile bodies (for example size 4.5 use 4x4 tiles for occupied)
; => only now there are no >1 tile entities anyway
(defn- rectangle->occupied-cells [grid {:keys [left-bottom width height] :as rectangle}]
  (if (or (> (float width) 1) (> (float height) 1))
    (grid/rectangle->cells grid rectangle)
    [(grid [(int (+ (float (left-bottom 0)) (/ (float width) 2)))
            (int (+ (float (left-bottom 1)) (/ (float height) 2)))])]))

(defn- set-occupied-cells! [grid eid]
  (let [cells (rectangle->occupied-cells grid @eid)]
    (doseq [cell cells]
      (assert (not (get (:occupied @cell) eid)))
      (swap! cell update :occupied conj eid))
    (swap! eid assoc ::occupied-cells cells)))

(defn- remove-from-occupied-cells! [eid]
  (doseq [cell (::occupied-cells @eid)]
    (assert (get (:occupied @cell) eid))
    (swap! cell update :occupied disj eid)))

(defn- add-entity! [{:keys [entity-ids content-grid grid]} eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))

  (content-grid/update-entity! content-grid eid)

  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (set-cells! grid eid)
  (when (:collides? @eid)
    (set-occupied-cells! grid eid)))

(defn- remove-entity! [{:keys [entity-ids content-grid grid]} eid]
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))

  (content-grid/remove-entity! eid)

  (remove-from-cells! eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)))

(defn position-changed! [{:keys [content-grid grid]} eid]
  (content-grid/update-entity! content-grid eid)

  (remove-from-cells! eid)
  (set-cells! grid eid)
  (when (:collides? @eid)
    (remove-from-occupied-cells! eid)
    (set-occupied-cells! grid eid)))

(defn- remove-destroyed-entities! [{:keys [entity-ids] :as world}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (remove-entity! world eid)
    (doseq [component @eid]
      (entity/destroy! component eid))))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def z-orders [:z-order/on-ground
               :z-order/ground
               :z-order/flying
               :z-order/effect])

(def render-z-order (define-order z-orders))

(defrecord Body [position
                 left-bottom
                 width
                 height
                 half-width
                 half-height
                 radius
                 collides?
                 z-order
                 rotation-angle])

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v])))
          {}
          components))

(def id-counter (atom 0))

(defn- spawn-entity [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (swap! id-counter inc))
                                      create-vs))))]
    (add-entity! ctx/world eid)
    (doseq [component @eid]
      (entity/create! component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn spawn-audiovisual [position {:keys [tx/sound entity/animation]}]
  (sound/play! sound)
  (spawn-entity position
                effect-body-props
                {:entity/animation animation
                 :entity/delete-after-animation-stopped? true}))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn spawn-creature [{:keys [position creature-id components]}]
  (let [props (db/build ctx/db creature-id)]
    (spawn-entity position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn spawn-item [position item]
  (spawn-entity position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [position faction duration]
  (spawn-entity position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (timer/create ctx/elapsed-time duration)
                  :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (spawn-entity start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn spawn-projectile [{:keys [position direction faction]}
                        {:keys [entity/image
                                projectile/max-range
                                projectile/speed
                                entity-effects
                                projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity position
                  {:width size
                   :height size
                   :z-order :z-order/flying
                   :rotation-angle (v/angle-from-vector direction)}
                  {:entity/movement {:direction direction
                                     :speed speed}
                   :entity/image image
                   :entity/faction faction
                   :entity/delete-after-duration (/ max-range speed)
                   :entity/destroy-audiovisual :audiovisuals/hit-wall
                   :entity/projectile-collision {:entity-effects entity-effects
                                                 :piercing? piercing?}})))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [grid position faction]
  (->> {:position position
        :radius shout-radius}
       (grid/circle->entities grid)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [{:keys [grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn world-item? []
  (not (cdq.stage/mouse-on-actor? ctx/stage)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [entity]
  (placement-point (:position entity)
                   (graphics/world-mouse-position ctx/graphics)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn- cache-active-entities
  "Expensive operation.

  Active entities are those which are nearby the position of the player and about one screen away."
  [{:keys [content-grid] :as world}]
  (assoc world :active-entities (content-grid/active-entities content-grid @ctx/player-eid)))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? (:world-viewport ctx/graphics) target))
       (not (and los-checks?
                 (raycaster/blocked? (:raycaster ctx/world) (:position source) (:position target))))))

(defn creatures-in-los-of-player [{:keys [active-entities]}]
  (->> active-entities
       (filter #(:entity/species @%))
       (filter #(line-of-sight? @ctx/player-eid @%))
       (remove #(:entity/player? @%))))

(defn- spawn-enemies! []
  (doseq [props (for [[position creature-id] (tiled/positions-with-property (:tiled-map ctx/world) :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature (update props :position tile->middle))))

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor-key (state/cursor new-state-obj)]
                                                     (graphics/set-cursor! ctx/graphics cursor-key)))
                                 :skill-added! (fn [skill]
                                                 (cdq.stage/add-skill! ctx/stage skill))
                                 :skill-removed! (fn [skill]
                                                   (cdq.stage/remove-skill! ctx/stage skill))
                                 :item-set! (fn [inventory-cell item]
                                              (cdq.stage/set-item! ctx/stage inventory-cell item))
                                 :item-removed! (fn [inventory-cell]
                                                  (cdq.stage/remove-item! ctx/stage inventory-cell))}
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage (cdq.stage/create!))
  (bind-root #'ctx/world (cdq.g.world/create ((requiring-resolve world-fn)
                                              (db/build-all ctx/db :properties/creatures))))
  (spawn-enemies!)
  (bind-root #'ctx/player-eid (spawn-creature (player-entity-props (:start-position ctx/world)))))

(bind-root #'ctx/reset-game! reset-game!)

(def ^:private explored-tile-color (color/create 0.5 0.5 0.5 1))

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
            base-color (if explored? explored-tile-color color/black)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? color/white base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              color/white))))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [g ctx/graphics
        cam (:camera (:world-viewport g))
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/draw-grid g
                          (int left-x) (int bottom-y)
                          (inc (int (:width  (:world-viewport g))))
                          (+ 2 (int (:height (:world-viewport g))))
                          1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell ((:grid ctx/world) [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/draw-filled-rectangle g x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [g]
  (let [position (graphics/world-mouse-position g)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/draw-rectangle g x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [g]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position g))
          cell ((:grid ctx/world) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/draw-rectangle g x y 1 1
                                 (case (:movement @cell)
                                   :air  [1 1 0 0.5]
                                   :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test ctx/graphics)
  (highlight-mouseover-tile ctx/graphics))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/draw-rectangle g x y (:width entity) (:height entity) color)))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
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
  (let [entities (map deref (:active-entities ctx/world))
        player @ctx/player-eid
        g ctx/graphics]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect g entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity g))
       (catch Throwable t
         (draw-body-rect g entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (cdq.stage/mouse-on-actor? ctx/stage)
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities (:grid ctx/world)
                                                           (graphics/world-mouse-position ctx/graphics)))]
                    (->> render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'ctx/mouseover-eid new-eid)))

(def pausing? true)

(defn- pause-game? []
  (or #_error
      (and pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (input/key-just-pressed? gdx/input :p)
                    (input/key-pressed?      gdx/input :space))))))

(defn- update-potential-fields! [{:keys [potential-field-cache
                                         grid
                                         active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.world.potential-fields/tick potential-field-cache
                                     grid
                                     faction
                                     active-entities
                                     max-iterations)))

(defn- tick-entities! [{:keys [active-entities]}]
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
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (stage/add-actor! ctx/stage (error-window/create t))
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! [camera]
  (let [zoom-speed 0.025]
    (when (input/key-pressed? gdx/input :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? gdx/input :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! [stage]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? gdx/input (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? gdx/input :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible! % false) windows)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (bind-root #'ctx/db (cdq.g.db/create))
    (lwjgl/application!
     (:application config)
     {:create! (fn []
                 (bind-root #'ctx/assets   (assets/create (:assets config)))
                 (bind-root #'ctx/graphics (cdq.g.graphics/create (:graphics config)))
                 (reset-game! (:world-fn config)))
      :dispose! (fn []
                  (dispose! ctx/assets)
                  (dispose! ctx/graphics)
                  ; TODO vis-ui dispose
                  ; TODO dispose world tiled-map/level resources?
                  )
      :render! (fn []
                 (alter-var-root #'ctx/world cache-active-entities)
                 (graphics/set-camera-position! ctx/graphics (:position @ctx/player-eid))
                 (screen-utils/clear! color/black)
                 (graphics/draw-tiled-map ctx/graphics
                                          (:tiled-map ctx/world)
                                          (tile-color-setter (:raycaster ctx/world)
                                                             (:explored-tile-corners ctx/world)
                                                             (camera/position (:camera (:world-viewport ctx/graphics)))))
                 (graphics/draw-on-world-view! ctx/graphics
                                               (fn []
                                                 (draw-before-entities!)
                                                 (render-entities!)
                                                 (draw-after-entities!)))
                 (stage/draw! ctx/stage)
                 (stage/act! ctx/stage)
                 (state/manual-tick (entity/state-obj @ctx/player-eid))
                 (update-mouseover-entity!)
                 (bind-root #'ctx/paused? (pause-game?))
                 (when-not ctx/paused?
                   (let [delta-ms (min (gdx.graphics/delta-time gdx/graphics) max-delta)]
                     (alter-var-root #'ctx/elapsed-time + delta-ms)
                     (bind-root #'ctx/delta-time delta-ms))
                   (update-potential-fields! ctx/world)
                   (tick-entities! ctx/world))

                 ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                 (remove-destroyed-entities! ctx/world)

                 (camera-controls! (:camera (:world-viewport ctx/graphics)))
                 (window-controls! ctx/stage))
      :resize! (fn [width height]
                 (graphics/resize! ctx/graphics width height))})))
