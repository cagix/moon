(ns anvil.lifecycle.render
  (:require [anvil.impl.effects :as effect-impl]
            [anvil.component :refer [render-below
                                     render-default
                                     render-above
                                     render-info
                                     render-effect]]
            [anvil.effect :as effect]
            [anvil.entity.body :as body]
            [anvil.entity.faction :as faction]
            [anvil.entity.hitpoints :as hp]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [anvil.item-on-cursor :refer [world-item? item-place-position]]
            [gdl.math.shapes :refer [circle->outer-rectangle]]
            [anvil.world :as world :refer [finished-ratio line-of-sight?]]
            [gdl.val-max :as val-max]
            [gdl.utils :refer [sort-by-order pretty-pst]]
            [anvil.lifecycle.potential-fields :refer [factions-iterations]]))

(defmethod render-effect :effects/target-all [_ {:keys [effect/source]}]
  (let [source* @source]
    (doseq [target* (map deref (world/creatures-in-los-of-player))]
      (g/line (:position source*) #_(start-point source* target*)
              (:position target*)
              [1 0 0 0.5]))))

(defmethod render-effect :effects/target-entity
  [[_ {:keys [maxrange]}] {:keys [effect/source effect/target]}]
  (when target
    (let [source* @source
          target* @target]
      (g/line (effect-impl/start-point source* target*)
              (effect-impl/end-point source* target* maxrange)
              (if (effect-impl/in-range? source* target* maxrange)
                [1 0 0 0.5]
                [1 1 0 0.5])))))

(defn- render-effects [ctx effects]
  (run! #(render-effect % ctx) effects))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [[width height] (:world-unit-dimensions image)
        _ (assert (= width height))
        radius (/ (float width) 2)
        y (+ (float y) (float (:half-height entity)) (float 0.15))
        center [x (+ y radius)]]
    (g/filled-circle center radius [1 1 1 0.125])
    (g/sector center radius
              90 ; start-angle
              (* (float action-counter-ratio) 360) ; degree
              [1 1 1 0.5])
    (g/draw-image image [(- (float x) radius) y])))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

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

(defn- draw-hpbar [{:keys [position width half-width half-height]}
                   ratio]
  (let [[x y] position]
    (let [x (- x half-width)
          y (+ y half-height)
          height (g/pixels->world-units 5)
          border (g/pixels->world-units borders-px)]
      (g/filled-rectangle x y width height g/black)
      (g/filled-rectangle (+ x border)
                          (+ y border)
                          (- (* width ratio)
                             (* 2 border))
                          (- height
                             (* 2 border))
                          (hpbar-color ratio)))))

(defn- geom-test []
  (let [position (g/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (g/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (world/circle->cells circle))]
      (g/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (g/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam g/camera
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (g/grid (int left-x) (int bottom-y)
              (inc (int g/world-viewport-width))
              (+ 2 (int g/world-viewport-height))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (world/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (g/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (g/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (g/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (g/world-mouse-position))
          cell (world/grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (g/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn- debug-render-before-entities []
  (tile-debug))

(defn- debug-render-after-entities []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private explored-tile-color (g/->color 0.5 0.5 0.5 1))

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

(defn- tile-color-setter* [light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @world/explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color g/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (world/ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? g/white base-color)
        (do (when-not explored?
              (swap! world/explored-tile-corners assoc (mapv int position) true))
            g/white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) g/white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

(defmethod render-below :stunned [_ entity]
  (g/circle (:position entity) 0.5 [1 1 1 0.6]))

(defmethod render-below :player-item-on-cursor [[_ {:keys [item]}] entity]
  (when (world-item?)
    (g/draw-centered (:entity/image item)
                     (item-place-position entity))))

(defmethod render-below :entity/mouseover? [_ {:keys [entity/faction] :as entity}]
  (let [player @world/player-eid]
    (g/with-line-width 3
      #(g/ellipse (:position entity)
                  (:half-width entity)
                  (:half-height entity)
                  (cond (= faction (faction/enemy player))
                        enemy-color
                        (= faction (:entity/faction player))
                        friendly-color
                        :else
                        neutral-color)))))

(defmethod render-default :entity/clickable
  [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (g/draw-text {:text text
                    :x x
                    :y (+ y (:half-height entity))
                    :up? true}))))

(defmethod render-default :entity/image [[_ image] entity]
  (g/draw-rotated-centered image
                           (or (:rotation-angle entity) 0)
                           (:position entity)))

(defmethod render-default :entity/line-render [[_ {:keys [thick? end color]}] entity]
  (let [position (:position entity)]
    (if thick?
      (g/with-line-width 4
        #(g/line position end color))
      (g/line position end color))))

; TODO draw opacity as of counter ratio?
(defmethod render-above :entity/temp-modifier [_ entity]
  (g/filled-circle (:position entity) 0.5 [0.5 0.5 0.5 0.4]))

(defmethod render-above :entity/string-effect
  [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (g/pixels->world-units 5))
                  :scale 2
                  :up? true})))

(defmethod render-above :npc-sleeping [_ entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text "zzz"
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true})))

(defmethod render-info :entity/hp [_ entity]
  (let [ratio (val-max/ratio (hp/->value entity))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar entity ratio))))

(defmethod render-info :active-skill [[_ {:keys [skill effect-ctx counter]}] entity]
  (let [{:keys [entity/image skill/effects]} skill]
    (draw-skill-image image entity (:position entity) (finished-ratio counter))
    (render-effects (effect/check-update-ctx effect-ctx) effects)))

(defn- render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @world/player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              body/render-z-order)
            system [render-below
                    render-default
                    render-above
                    render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn render-world []
  ; FIXME position DRY
  (cam/set-position! g/camera
                     (:position @world/player-eid))
  ; FIXME position DRY
  (g/draw-tiled-map world/tiled-map
                    (tile-color-setter (cam/position g/camera)))
  (g/draw-on-world-view (fn []
                          (debug-render-before-entities)
                          ; FIXME position DRY (from player)
                          (render-entities (map deref (world/active-entities)))
                          (debug-render-after-entities))))
