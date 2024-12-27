(ns cdq.context
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]
            [anvil.level :refer [generate-level]]
            [anvil.widgets :as widgets]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [cdq.potential-fields :as potential-fields]
            [cdq.tile-color-setter :as tile-color-setter]
            [clojure.gdx :as gdx :refer [play key-pressed? key-just-pressed? clear-screen black]]
            [data.grid2d :as g2d]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.graphics.camera :as cam]
            [gdl.math.raycaster :as raycaster]
            [gdl.math.shapes :refer [circle->outer-rectangle]]
            [gdl.math.vector :as v]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui ButtonGroup)))

(defmethods ::tiled-map
  (component/dispose [[_ tiled-map]] ; <- this context cleanup, also separate world-cleanup when restarting ?!
    (tiled/dispose tiled-map)))

(defmethods ::error
  (component/->v [_ _c]
    nil))

(defmethods ::explored-tile-corners
  (component/->v [_ {::keys [tiled-map]}]
    (atom (g2d/create-grid
           (tiled/tm-width  tiled-map)
           (tiled/tm-height tiled-map)
           (constantly false)))))

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
    (some #(not= % eid) occupied)) ; contains? faster?

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

(defmethods ::grid
  (component/->v [_ {::keys [tiled-map]}]
    (g2d/create-grid
     (tiled/tm-width tiled-map)
     (tiled/tm-height tiled-map)
     (fn [position]
       (atom (->grid-cell position
                          (case (tiled/movement-property tiled-map position)
                            "none" :none
                            "air"  :air
                            "all"  :all)))))))

(defmethods ::content-grid
  (component/->v [[_ {:keys [cell-size]}] {::keys [tiled-map]}]
    (content-grid/create {:cell-size cell-size
                          :width  (tiled/tm-width  tiled-map)
                          :height (tiled/tm-height tiled-map)})))

(defmethods ::entity-ids
  (component/->v [_ _c]
    (atom {})))

(defmethods ::elapsed-time
  (component/->v [_ _c]
    0))

; TODO this passing w. world props ...
; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(declare creature)

(defmethods ::player-eid
  (component/->v [_ {::keys [start-position] :as c}]
    (assert start-position)
    (creature c (player-entity-props start-position))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defmethods ::raycaster
  (component/->v [_ {::keys [grid]}]
    (let [width  (g2d/width  grid)
          height (g2d/height grid)
          arr (make-array Boolean/TYPE width height)]
      (doseq [cell (g2d/cells grid)]
        (set-arr arr @cell grid/blocks-vision?))
      [arr width height])))

(defn- widgets-windows [c]
  (ui/group {:id :windows
             :actors [(widgets/entity-info-window c)
                      (widgets/inventory c)]}))

(defn- widgets-player-state-draw-component [_context]
  (ui-actor {:draw #(component/draw-gui-view (entity/state-obj @(::player-eid %))
                                             %)}))

(defn widgets [c]
  [(widgets/dev-menu c)
   (widgets/action-bar-table c)
   (widgets/hp-mana-bar c)
   (widgets-windows c)
   (widgets-player-state-draw-component c)
   (widgets/player-message)])

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(defn grid-cell [{::keys [grid]} position]
  (grid position))

(defn rectangle->cells [{::keys [grid]} rectangle]
  (grid/rectangle->cells grid rectangle))

(defn circle->cells [{::keys [grid]} circle]
  (grid/circle->cells grid circle))

(defn circle->entities [{::keys [grid]} circle]
  (grid/circle->entities grid circle))

(defn cached-adjacent-cells [{::keys [grid]} cell]
  (grid/cached-adjacent-cells grid cell))

(defn point->entities [{::keys [grid]} position]
  (grid/point->entities grid position))

(defn ray-blocked? [{::keys [raycaster]} start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [{::keys [raycaster]} start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

(defn timer [{::keys [elapsed-time]} duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{::keys [elapsed-time]}
                {:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset-timer [{::keys [elapsed-time]}
                   {:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed-time duration)))

(defn finished-ratio [{::keys [elapsed-time] :as c}
                      {:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? c counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(defn mouseover-entity [{::keys [mouseover-eid]}]
  (and mouseover-eid
       @mouseover-eid))

(defn all-entities [{::keys [entity-ids]}]
  (vals @entity-ids))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (:camera viewport))
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
(defn line-of-sight? [{:keys [gdl.context/world-viewport] :as c} source target]
  (and (or (not (:entity/player? source))
           (on-screen? world-viewport target))
       (not (and los-checks?
                 (ray-blocked? c (:position source) (:position target))))))

; this as protocols & impl implements it? same with send-event ?
; so we could add those protocols to 'entity'?
; => also add render stuff
; so each component is together all stuff (but question is if we have open data)
(defn add-text-effect [entity c text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(reset-timer c %)))
           {:text text
            :counter (timer c 0.4)})))

(defn active-entities [{::keys [content-grid player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn- add-entity [{::keys [content-grid grid entity-ids]} eid]
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (content-grid/add-entity content-grid eid)
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (grid/add-entity grid eid))

(defn- remove-entity [{::keys [entity-ids]} eid]
  (content-grid/remove-entity eid)
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (grid/remove-entity         eid))

(defn position-changed [{::keys [content-grid grid]} eid]
  (content-grid/entity-position-changed content-grid eid)
  (grid/entity-position-changed grid eid))

; setting a min-size for colliding bodies so movement can set a max-speed for not
; skipping bodies at too fast movement
; TODO assert at properties load
(def minimum-size 0.39) ; == spider smallest creature size.

(def ^:private z-orders [:z-order/on-ground
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

(defn- create-vs [components c]
  (reduce (fn [m [k v]]
            (assoc m k (component/->v [k v] c)))
          {}
          components))

(defn spawn-entity [c position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      create-body
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs c)))))]
    (add-entity c eid)
    (doseq [component @eid]
      (component/create component eid c))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn audiovisual [c position {:keys [tx/sound entity/animation]}]
  (play sound)
  (spawn-entity c
                position
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

(defn creature [c {:keys [position creature-id components]}]
  (let [props (c/build c creature-id)]
    (spawn-entity c
                  position
                  (->body (:entity/body props))
                  (-> props
                      (dissoc :entity/body)
                      (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                      (safe-merge components)))))

(defn item [c position item]
  (spawn-entity c
                position
                {:width 0.75
                 :height 0.75
                 :z-order :z-order/on-ground}
                {:entity/image (:entity/image item)
                 :entity/item item
                 :entity/clickable {:type :clickable/item
                                    :text (:property/pretty-name item)}}))

(defn delayed-alert [c position faction duration]
  (spawn-entity c
                position
                effect-body-props
                {:entity/alert-friendlies-after-duration
                 {:counter (timer c duration)
                  :faction faction}}))

(defn line-render [c {:keys [start end duration color thick?]}]
  (spawn-entity c
                start
                effect-body-props
                #:entity {:line-render {:thick? thick? :end end :color color}
                          :delete-after-duration duration}))

(defn projectile-size [projectile]
  {:pre [(:entity/image projectile)]}
  (first (:world-unit-dimensions (:entity/image projectile))))

(defn projectile [c
                  {:keys [position direction faction]}
                  {:keys [entity/image
                          projectile/max-range
                          projectile/speed
                          entity-effects
                          projectile/piercing?] :as projectile}]
  (let [size (projectile-size projectile)]
    (spawn-entity c
                  position
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

(defn- remove-destroyed-entities [c]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (all-entities c))]
    (remove-entity c eid)
    (doseq [component @eid]
      (component/destroy component eid c))))

(defn creatures-in-los-of-player [{::keys [player-eid] :as c}]
  (->> (active-entities c)
       (filter #(:entity/species @%))
       (filter #(line-of-sight? c @player-eid @%))
       (remove #(:entity/player? @%))))

(def ^:private shout-radius 4)

(defn friendlies-in-radius [c position faction]
  (->> {:position position
        :radius shout-radius}
       (circle->entities c)
       (filter #(= (:entity/faction @%) faction))))

(defn nearest-enemy [{::keys [grid]} entity]
  (grid/nearest-entity @(grid (entity/tile entity))
                       (entity/enemy entity)))

(defn get-inventory [c]
  (get (:windows (c/stage c)) :inventory-window))

(defn get-action-bar [c]
  (let [group (:ui/action-bar (:action-bar-table (c/stage c)))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn selected-skill [c]
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar c)))]
    (actor/user-object skill-button)))

(def player-message-duration-seconds 1.5)

(def message-to-player nil)

(defn show-player-msg [message]
  (bind-root message-to-player {:message message :counter 0}))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [gdl.context/viewport] :as c}
                  {:keys [title text button-text on-click]}]
  (assert (not (::modal (c/stage c))))
  (c/add-actor c
               (ui/window {:title title
                           :rows [[(ui/label text)]
                                  [(ui/text-button button-text
                                                   (fn []
                                                     (Actor/.remove (::modal (c/stage c)))
                                                     (on-click)))]]
                           :id ::modal
                           :modal? true
                           :center-position [(/ (:width viewport) 2)
                                             (* (:height viewport) (/ 3 4))]
                           :pack? true})))

(defn- render-tiled-map [{::keys [raycaster explored-tile-corners] :as c}
                         tiled-map
                         light-position]
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter/create raycaster
                                              explored-tile-corners
                                              (atom {})
                                              light-position)))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- render-debug-before-entities [{:keys [gdl.context/world-viewport]
                                      ::keys [factions-iterations]
                                      :as c}]
  (let [cam (:camera world-viewport)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (c/grid c
              (int left-x) (int bottom-y)
              (inc (int (:width  world-viewport)))
              (+ 2 (int (:height world-viewport)))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid-cell c [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (c/filled-rectangle c x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (c/filled-rectangle c x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (c/filled-rectangle c x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [c]
  (let [position (c/world-mouse-position c)
        radius 0.8
        circle {:position position :radius radius}]
    (c/circle c position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells c circle))]
      (c/rectangle c x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (c/rectangle c x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [c]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (c/world-mouse-position c))
          cell (grid-cell c [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (c/rectangle c x y 1 1
                     (case (:movement @cell)
                       :air  [1 1 0 0.5]
                       :none [1 0 0 0.5]))))))

(defn- render-debug-after-entities [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))

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

(defn- render-entities [{::keys [player-eid] :as c} entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [component/render-below
                    component/render-default
                    component/render-above
                    component/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? c player entity))]
      (render-entity! c system entity))))

(defn- render-world [{:keys [gdl.context/world-viewport]
                      ::keys [tiled-map player-eid]
                      :as c}]
  ; FIXME position DRY
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  ; FIXME position DRY
  (render-tiled-map c tiled-map (cam/position (:camera world-viewport)))
  (c/draw-on-world-view c
                        (fn [c]
                          (render-debug-before-entities c)
                          ; FIXME position DRY (from player)
                          (render-entities c (map deref (active-entities c)))
                          (render-debug-after-entities c))))

(defn- check-player-input [{::keys [player-eid] :as c}]
  (component/manual-tick (entity/state-obj @player-eid)
                         c))

(defmethod component/pause-game? :active-skill          [_] false)
(defmethod component/pause-game? :stunned               [_] false)
(defmethod component/pause-game? :player-moving         [_] false)
(defmethod component/pause-game? :player-item-on-cursor [_] true)
(defmethod component/pause-game? :player-idle           [_] true)
(defmethod component/pause-game? :player-dead           [_] true)

(defn- update-paused-state [{::keys [player-eid error] :as c} pausing?]
  (assoc c ::paused? (or error
                         (and pausing?
                              (component/pause-game? (entity/state-obj @player-eid))
                              (not (controls/unpaused? c))))))

(defn- calculate-mouseover-eid [{::keys [player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities c (c/world-mouse-position c)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn- update-mouseover-entity [{::keys [mouseover-eid] :as c}]
  (let [new-eid (if (c/mouse-on-actor? c)
                  nil
                  (calculate-mouseover-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c ::mouseover-eid new-eid)))

(defn- update-time [c]
  (let [delta-ms (min (gdx/delta-time c) max-delta-time)]
    (-> c
        (update ::elapsed-time + delta-ms)
        (assoc ::delta-time delta-ms))))

(def ^:private pf-cache (atom nil))

(defn- tick-potential-fields [{::keys [factions-iterations grid] :as c}]
  (let [entities (active-entities c)]
    (doseq [[faction max-iterations] factions-iterations]
      (potential-fields/tick pf-cache
                             grid
                             faction
                             entities
                             max-iterations)))
  c)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [c eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/tick [k v] eid c))
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

(def ^:private zoom-speed 0.025)

(defn- check-camera-controls [c camera]
  (when (key-pressed? c :minus)  (cam/inc-zoom camera    zoom-speed))
  (when (key-pressed? c :equals) (cam/inc-zoom camera (- zoom-speed))) )

(defn- check-window-hotkeys [c {:keys [controls/window-hotkeys]} stage]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? c (get window-hotkeys window-id))]
    (toggle-visible! (get (:windows stage) window-id))))

(defn- close-all-windows [stage]
  (let [windows (children (:windows stage))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

(defn- check-ui-key-listeners [c {:keys [controls/close-windows-key] :as controls} stage]
  (check-window-hotkeys c controls stage)
  (when (key-just-pressed? c close-windows-key)
    (close-all-windows stage)))

(defn- tick-context [{:keys [gdl.context/world-viewport] :as c} pausing?]
  (check-player-input c)
  (let [c (-> c
              update-mouseover-entity
              (update-paused-state pausing?))
        c (if (::paused? c)
            c
            (-> c
                update-time
                tick-potential-fields
                tick-entities))]
    (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
    (check-camera-controls c (:camera world-viewport))
    (check-ui-key-listeners c
                            {:controls/close-windows-key controls/close-windows-key
                             :controls/window-hotkeys    controls/window-hotkeys}
                            (c/stage c))
    c))

(defn- spawn-enemies [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (creature c (update props :position tile->middle))))

(defn- world-components [c world-id]
  (let [{:keys [tiled-map start-position]} (generate-level c (c/build c world-id))] ; ?
    [[:cdq.context/tiled-map tiled-map]
     [:cdq.context/start-position start-position]
     [:cdq.context/grid nil]
     [:cdq.context/explored-tile-corners nil]
     [:cdq.context/content-grid {:cell-size 16}]
     [:cdq.context/entity-ids nil]
     [:cdq.context/raycaster nil]
     [:cdq.context/factions-iterations {:good 15 :evil 5}]
     ; "The elapsed in-game-time in seconds (not counting when game is paused)."
     [:cdq.context/elapsed-time nil] ; game speed config!?
     [:cdq.context/player-eid nil] ; pass props
     ;:mouseover-eid nil ; ?
     ;:delta-time "The game logic update delta-time in ms."
     ;(bind-root world/delta-time nil) ?
     [:cdq.context/error nil]]))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- add-game-state [c world-id]
  (c/reset-stage c (widgets c)) ; pass to stage . simply! and at reset do the same
  ; stage required before spawn-player because inventory .... make explicit those dependencies ... ?
  (let [c (c/create-into c (world-components c world-id))] ; same ...
    (when spawn-enemies?
      (spawn-enemies c (:cdq.context/tiled-map c))) ; ??? creature-props!
    c))

(defn- create-context [gdx-context {:keys [requires gdl world]}]
  (run! require requires)
  (let [context (c/create-into gdx-context gdl)]
    (add-game-state context world)))

; TODO unused
(defn- dispose-game-state [{::keys [tiled-map]}]
  (when tiled-map
    (tiled/dispose tiled-map)))

(def ^:private ^:dbg-flag pausing? true)

(defrecord Context []
  app/Context
  (dispose [context]
    (c/cleanup context))

  (render [context]
    (clear-screen black)
    (render-world context)
    (let [stage (c/stage context)]
      (set! (.applicationState stage) context)
      (.draw stage)
      (.act stage))
    (tick-context context pausing?))

  (resize [context width height]
    (c/resize context width height)))

(defn -main []
  (let [config (read-edn-resource "app.edn")]
    (app/start (:app config)
               #(map->Context (create-context % (:context config))))))
