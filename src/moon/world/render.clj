(in-ns 'moon.world)

(defn- init-explored-tile-corners [width height]
  (def explored-tile-corners (atom (g2d/create-grid width height (constantly false)))))

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

(defn- ->tile-color-setter [light-cache light-position]
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            color/white)))))

(declare tiled-map)

(defn clear-tiled-map []
  (when (bound? #'tiled-map)
    (dispose tiled-map)))

(defn init-tiled-map [tm]
  (clear-tiled-map)
  (.bindRoot #'tiled-map tm))

(defn render-tiled-map! [light-position]
  (g/draw-tiled-map tiled-map
                    (->tile-color-setter (atom nil) light-position))
  #_(reset! do-once false))
