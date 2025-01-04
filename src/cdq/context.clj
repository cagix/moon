(ns cdq.context
  (:require [gdl.math.shapes :refer [circle->outer-rectangle]]
            [gdl.context.timer :as timer]
            [gdl.graphics.animation :as animation]
            [cdq.fsm :as fsm]
            [cdq.entity :as entity]
            [gdl.error :refer [pretty-pst]]
            [cdq.entity.state :as state]
            [cdq.tile-color-setter :as tile-color-setter]
            [anvil.level :refer [generate-level]]
            [anvil.widgets :as widgets]
            [anvil.widgets.inventory :as inventory]
            [cdq.content-grid :as content-grid]
            [cdq.grid :as grid]
            [clojure.gdx :as gdx :refer [play key-pressed? key-just-pressed? clear-screen black]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui.button-group :as button-group]
            [gdl.utils :refer [defsystem defcomponent tile->middle readable-number dev-mode? define-order sort-by-order safe-merge]]
            [data.grid2d :as g2d]
            [gdl.app :as app]
            [gdl.context :as c]
            [cdq.context.info :as info]
            [gdl.graphics.camera :as cam]
            [gdl.math.raycaster :as raycaster]
            [cdq.potential-fields :as potential-fields]
            [clojure.gdx.math.vector2 :as v]
            [clojure.gdx.tiled :as tiled]
            [gdl.ui :as ui :refer [ui-actor]]
            [clojure.gdx.scene2d.actor :as actor]
            [gdl.ui.dev-menu :as dev-menu]
            [clojure.gdx.scene2d.group :as group]
            [gdl.val-max :as val-max]))

(defcomponent ::tiled-map
  (app/create [_ {::keys [level]}]
    (:tiled-map level))
  (app/dispose [[_ tiled-map]] ; <- this context cleanup, also separate world-cleanup when restarting ?!
    (tiled/dispose tiled-map)))

(defcomponent ::error
  (app/create [_ _c]
    nil))

(defcomponent ::explored-tile-corners
  (app/create [_ {::keys [tiled-map]}]
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

(defcomponent ::grid
  (app/create [_ {::keys [tiled-map]}]
    (g2d/create-grid
     (tiled/tm-width tiled-map)
     (tiled/tm-height tiled-map)
     (fn [position]
       (atom (->grid-cell position
                          (case (tiled/movement-property tiled-map position)
                            "none" :none
                            "air"  :air
                            "all"  :all)))))))

(defcomponent ::content-grid
  (app/create [[_ {:keys [cell-size]}] {::keys [tiled-map]}]
    (content-grid/create {:cell-size cell-size
                          :width  (tiled/tm-width  tiled-map)
                          :height (tiled/tm-height tiled-map)})))

(defcomponent ::entity-ids
  (app/create [_ _c]
    (atom {})))

(defcomponent :gdl.context.timer/elapsed-time
  (app/create [_ _c]
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

(defcomponent ::player-eid
  (app/create [_ {::keys [level] :as c}]
    (assert (:start-position level))
    (creature c (player-entity-props (:start-position level)))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defcomponent ::raycaster
  (app/create [_ {::keys [grid]}]
    (let [width  (g2d/width  grid)
          height (g2d/height grid)
          arr (make-array Boolean/TYPE width height)]
      (doseq [cell (g2d/cells grid)]
        (set-arr arr @cell grid/blocks-vision?))
      [arr width height])))

(declare mouseover-entity)

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text [c]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [entity (mouseover-entity c)]
                (info/text c [:property/pretty-name (:property/pretty-name entity)])
                "Entity Info"))
  (when-let [entity (mouseover-entity c)]
    (info/text c ; don't use select-keys as it loses Entity record type
               (apply dissoc entity disallowed-keys))))

(defn entity-info-window [{:keys [gdl.context/viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [(:width viewport) 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn [context]
                                               (.setText label (str (->label-text context)))
                                               (.pack window))}))
    window))

(defn- widgets-windows [c]
  (ui/group {:id :windows
             :actors [(entity-info-window c)
                      (inventory/create c)]}))

(defn- widgets-player-state-draw-component [_context]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(::player-eid %))
                                          %)}))

(def help-text
  "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause")

(defn- dev-menu-config [c]
  {:menus [{:label "World"
            :items (for [world (c/build-all c :properties/worlds)]
                     {:label (str "Start " (:property/id world))
                      :on-click
                      (fn [_context])
                      ;#(world/create % (:property/id world))

                      })}
           ; TODO fixme does not work because create world uses create-into which checks key is not preseent
           ; => look at cleanup-world/reset-state/ (camera not reset - mutable state be careful ! -> create new cameras?!)
           ; => also world-change should be supported, use component systems
           {:label "Help"
            :items [{:label help-text}]}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [c]
                                 (when-let [entity (mouseover-entity c)]
                                   (:entity/id entity)))
                    :icon "images/mouseover.png"}
                   {:label "elapsed-time"
                    :update-fn (fn [{:keys [gdl.context.timer/elapsed-time]}]
                                 (str (readable-number elapsed-time) " seconds"))
                    :icon "images/clock.png"}
                   {:label "paused?"
                    :update-fn ::paused?} ; TODO (def paused ::paused) @ cdq.context
                   {:label "GUI"
                    :update-fn c/mouse-position}
                   {:label "World"
                    :update-fn #(mapv int (c/world-mouse-position %))}
                   {:label "Zoom"
                    :update-fn #(cam/zoom (:camera (:gdl.context/world-viewport %))) ; TODO (def ::world-viewport)
                    :icon "images/zoom.png"}
                   {:label "FPS"
                    :update-fn gdx/frames-per-second
                    :icon "images/fps.png"}]})

(defn- dev-menu [c]
  (if dev-mode?
    (dev-menu/table c (dev-menu-config c))
    (ui-actor {})))

(defn- render-infostr-on-bar [c infostr x y h]
  (c/draw-text c
               {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn- hp-mana-bar [{:keys [gdl.context/viewport] :as c}]
  (let [rahmen      (c/sprite c "images/rahmen.png")
        hpcontent   (c/sprite c "images/hp.png")
        manacontent (c/sprite c "images/mana.png")
        x (/ (:width viewport) 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (c/draw-image c rahmen [x y])
                            (c/draw-image c
                                          (c/sub-sprite c
                                                        contentimage
                                                        [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar c (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn [{:keys [cdq.context/player-eid]}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana        player-entity) "MP")))})))

(defn widgets [c]
  [(dev-menu c)
   (widgets/action-bar-table c)
   (hp-mana-bar c)
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
               (update :counter #(timer/reset c %)))
           {:text text
            :counter (timer/create c 0.4)})))

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

(defn remove-entity [{::keys [entity-ids]} eid]
  (content-grid/remove-entity eid)
  (let [id (:entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (grid/remove-entity eid))

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

(defn- create-vs [components context]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] context)))
          {}
          components))

(defsystem create!)
(defmethod create! :default [_ eid c])

(let [cnt (atom 0)]
  (defn spawn-entity [c position body components]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        create-body
                        (safe-merge (-> components
                                        (assoc :entity/id (swap! cnt inc))
                                        (create-vs c)))))]
      (add-entity c eid)
      (doseq [component @eid]
        (create! component eid c))
      eid)))

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
                 {:counter (timer/create c duration)
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

(defn- action-bar-add-skill [c {:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (ui/image-button image (fn []) {:scale 2})]
    (actor/set-id button id)
    (ui/add-tooltip! button #(info/text % skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (group/add-actor! horizontal-group button)
    (button-group/add button-group button)
    nil))

(defn- action-bar-remove-skill [c {:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar c)
        button (get horizontal-group id)]
    (actor/remove button)
    (button-group/remove button-group button)
    nil))

(defn selected-skill [c]
  (when-let [skill-button (button-group/checked (:button-group (get-action-bar c)))]
    (actor/user-object skill-button)))

(defcomponent ::player-message
  (app/create [[_ {:keys [duration-seconds]}] _context]
    (atom {:duration-seconds duration-seconds})))

(defn show-player-msg [{::keys [player-message]} text]
  (swap! player-message assoc :text text :counter 0))

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
                                                     (actor/remove (::modal (c/stage c)))
                                                     (on-click)))]]
                           :id ::modal
                           :modal? true
                           :center-position [(/ (:width viewport) 2)
                                             (* (:height viewport) (/ 3 4))]
                           :pack? true})))

(defn- spawn-enemies [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (creature c (update props :position tile->middle))))

(defcomponent ::level
  (app/create [[_ world-id] c]
    (generate-level c (c/build c world-id))))

(defcomponent ::stage-actors
  (app/create [_ c]
    (c/reset-stage c (widgets c))))

(defcomponent ::spawn-enemies
  (app/create [_ c]
    (spawn-enemies c (::tiled-map c))))

(defcomponent ::requires
  (app/create [[_ namespaces] _context]
    (run! require namespaces)))

; TODO unused
(defn- dispose-game-state [{::keys [tiled-map]}]
  (when tiled-map
    (tiled/dispose tiled-map)))

(defn world-item? [c]
  (not (c/mouse-on-actor? c)))

; It is possible to put items out of sight, losing them.
; Because line of sight checks center of entity only, not corners
; this is okay, you have thrown the item over a hill, thats possible.
(defn- placement-point [player target maxrange]
  (v/add player
         (v/scale (v/direction player target)
                  (min maxrange
                       (v/distance player target)))))

(defn item-place-position [c entity]
  (placement-point (:position entity)
                   (c/world-mouse-position c)
                   ; so you cannot put it out of your own reach
                   (- (:entity/click-distance-tiles entity) 0.1)))

(defn send-event!
  ([c eid event]
   (send-event! c eid event nil))
  ([c eid event params]
   (when-let [fsm (:entity/fsm @eid)]
     (let [old-state-k (:state fsm)
           new-fsm (fsm/event fsm event)
           new-state-k (:state new-fsm)]
       (when-not (= old-state-k new-state-k)
         (let [old-state-obj (entity/state-obj @eid)
               new-state-obj [new-state-k (entity/create (if params
                                                           [new-state-k eid params]
                                                           [new-state-k eid])
                                                         c)]]
           (when (:entity/player? @eid)
             (when-let [cursor (state/cursor new-state-obj)]
               (c/set-cursor c cursor)))
           (swap! eid #(-> %
                           (assoc :entity/fsm new-fsm
                                  new-state-k (new-state-obj 1))
                           (dissoc old-state-k)))
           (state/exit  old-state-obj c)
           (state/enter new-state-obj c)))))))

(defn set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (cdq.inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (inventory/set-item-image-in-widget c cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (cdq.inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn remove-item [c eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (inventory/remove-item-from-widget c cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (cdq.inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (cdq.inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item c eid cell)
            (set-item c eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [c eid item]
  (let [[cell cell-item] (entity/can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (cdq.inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (cdq.inventory/stackable? item cell-item)
      (stack-item c eid cell item)
      (set-item c eid cell item))))

(defn draw-body-rect [c entity color]
  (let [[x y] (:left-bottom entity)]
    (c/rectangle c x y (:width entity) (:height entity) color)))

(defn check-player-input [{:keys [cdq.context/player-eid] :as c}]
  (state/manual-tick (entity/state-obj @player-eid)
                     c))

(defn set-camera-on-player-position [{:keys [gdl.context/world-viewport
                                             cdq.context/player-eid]}]
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid)))

(defn render-tiled-map [{:keys [gdl.context/world-viewport
                                cdq.context/tiled-map
                                cdq.context/raycaster
                                cdq.context/explored-tile-corners] :as c}]
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter/create raycaster
                                              explored-tile-corners
                                              (cam/position (:camera world-viewport)))))

(defn- calculate-mouseover-eid [{:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities c (c/world-mouse-position c)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? c player @%))
         first)))

(defn update-mouseover-entity [{:keys [cdq.context/mouseover-eid] :as c}]
  (let [new-eid (if (c/mouse-on-actor? c)
                  nil
                  (calculate-mouseover-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn update-paused-state [{:keys [cdq.context/player-eid error] :as c} pausing?]
  (assoc c :cdq.context/paused? (or error
                                    (and pausing?
                                         (state/pause-game? (entity/state-obj @player-eid))
                                         (not (or (key-just-pressed? c :p)
                                                  (key-pressed? c :space)))))))

(defn update-time [c]
  (let [delta-ms (min (gdx/delta-time c) max-delta-time)]
    (-> c
        (update :gdl.context.timer/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(def ^:private pf-cache (atom nil))

(defn tick-potential-fields [{:keys [cdq.context/factions-iterations
                                     cdq.context/grid] :as c}]
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
(defn tick-entities [c]
  (try (doseq [eid (active-entities c)]
         (try
          (doseq [k (keys @eid)]
            (try (when-let [v (k @eid)]
                   (entity/tick [k v] eid c))
                 (catch Throwable t
                   (throw (ex-info "entity-tick" {:k k} t)))))
          (catch Throwable t
            (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
       (catch Throwable t
         (c/error-window c t)
         #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  c)

(defn remove-destroyed-entities [c]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (all-entities c))]
    (remove-entity c eid)
    (doseq [component @eid]
      (entity/destroy component eid c))))

(def window-hotkeys
  {:inventory-window   :i
   :entity-info-window :e})

(defn- check-window-hotkeys [c]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? c (get window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows (c/stage c)) window-id))))

(defn- close-all-windows [stage]
  (let [windows (group/children (:windows stage))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible % false) windows))))

(def close-windows-key :escape)

(defn check-ui-key-listeners [c]
  (check-window-hotkeys c)
  (when (key-just-pressed? c close-windows-key)
    (close-all-windows (c/stage c))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn render-before-entities [{:keys [gdl.context/world-viewport
                                      cdq.context/factions-iterations]
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

(defn render-after-entities [c]
  #_(geom-test c)
  (highlight-mouseover-tile c))

(defn player-movement-vector [c]
  (c/WASD-movement-vector c))

(defn add-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(not (entity/has-skill? @eid skill))]}
  (when (:entity/player? @eid)
    (action-bar-add-skill c skill))
  (swap! eid assoc-in [:entity/skills id] skill))

(defn remove-skill [c eid {:keys [property/id] :as skill}]
  {:pre [(entity/has-skill? @eid skill)]}
  (when (:entity/player? @eid)
    (action-bar-remove-skill c skill))
  (swap! eid update :entity/skills dissoc id))

(defmethod create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k cdq.inventory/empty-inventory)
  (doseq [item items]
    (pickup-item c eid item)))

(defmethod create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (add-skill c eid skill)))

(defmethod create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (fsm/create fsm initial-state)
         initial-state (entity/create [initial-state eid] c)))
