(in-ns 'moon.world)

(defn- init-ids->eids []
  (def ^:private ids->eids {}))

(defn all-entities [] (vals ids->eids))
(defn get-entity [id] (get ids->eids id))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (g/world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (g/world-viewport-width))  2)))
     (<= ydist (inc (/ (float (g/world-viewport-height)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/draw-rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t 12))))

(defn- render-entities!
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player-entity @player]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                               first
                                               entity/render-order)
            system entity/render-systems
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player-entity entity))]
      (render-entity! system entity))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-system [eid]
  (try
   (doseq [k (keys @eid)]
     (when-let [v (k @eid)]
       (component/->handle (entity/tick [k v] eid))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities!
  "Calls tick system on all components of entities."
  [entities]
  (run! tick-system entities))
