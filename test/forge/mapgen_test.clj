(ns forge.mapgen-test
  (:require [clojure.level :refer [generate-level]]
            [clojure.modules :as modules]
            [clojure.graphics.color :as color]
            [clojure.graphics.shape-drawer :as sd]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.context :as c :refer [draw-tiled-map]]
            [clojure.input :as input]
            [clojure.graphics.camera :as cam]
            [clojure.tiled :as tiled]
            [clojure.maps.tiled.tmx-map-loader :as tmx-map-loader]
            [clojure.ui :refer [ui-actor text-button] :as ui]
            [clojure.scene2d.group :refer [add-actor!]]))

(def state (atom nil))

(defn- show-whole-map! [camera tiled-map]
  (cam/set-position camera
                    [(/ (tiled/tm-width  tiled-map) 2)
                     (/ (tiled/tm-height tiled-map) 2)])
  (cam/set-zoom camera
                (cam/calculate-zoom camera
                                    :left [0 0]
                                    :top [0 (tiled/tm-height tiled-map)]
                                    :right [(tiled/tm-width tiled-map) 0]
                                    :bottom [0 0])))

(defn- current-data [] ; TODO just use vars
  #_(-> (screen/current)
        :sub-screen
        :current-data))

(def ^:private infotext
  "L: grid lines
  M: movement properties
  zoom: minus,equals
  ESCAPE: leave
  direction keys: move")

(defn- map-infos ^String [c]
  (let [tile (mapv int (c/world-mouse-position c))
        {:keys [tiled-map
                area-level-grid]} @(current-data)]
    (->> [infotext
          (str "Tile " tile)
          (when-not area-level-grid
            (str "Module " (mapv (comp int /)
                                 (c/world-mouse-position c)
                                 [modules/width modules/height])))
          (when area-level-grid
            (str "Creature id: " (tiled/property-value tiled-map :creatures tile :id)))
          (when area-level-grid
            (let [level (get area-level-grid tile)]
              (when (number? level)
                (str "Area level:" level))))
          (str "Movement properties " (tiled/movement-property tiled-map tile) "\n"
               (apply vector (tiled/movement-properties tiled-map tile)))]
         (remove nil?)
         (str/join "\n"))))

(defn- ->info-window [{:keys [clojure.graphics/ui-viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info" :rows [[label]]})]
    (add-actor! window (ui-actor {:act #(do
                                         (.setText label (map-infos %))
                                         (.pack window))}))
    (.setPosition window 0 (:height ui-viewport)) window))

(def ^:private camera-movement-speed 1)

; TODO textfield takes control !
; PLUS symbol shift & = symbol on keyboard not registered
(defn- camera-controls [camera]
  (let [apply-position (fn [idx f]
                         (cam/set-position camera
                                           (update (cam/position camera)
                                                   idx
                                                   #(f % camera-movement-speed))))]
    (if (input/key-pressed? :left)  (apply-position 0 -))
    (if (input/key-pressed? :right) (apply-position 0 +))
    (if (input/key-pressed? :up)    (apply-position 1 +))
    (if (input/key-pressed? :down)  (apply-position 1 -))))

(defn- render-on-map [{:keys [clojure.graphics/world-viewport
                              clojure.graphics/shape-drawer] :as c}]
  (let [{:keys [tiled-map
                area-level-grid
                start-position
                show-movement-properties
                show-grid-lines]} @(current-data)
        visible-tiles (cam/visible-tiles (:camera world-viewport))
        [x y] (mapv int (c/world-mouse-position c))
        sd shape-drawer]
    (sd/rectangle sd x y 1 1 :white)
    (when start-position
      (sd/filled-rectangle sd (start-position 0) (start-position 1) 1 1 [1 0 1 0.9]))
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [prop (tiled/movement-property tiled-map [x y])]]
        (sd/filled-circle sd [(+ x 0.5) (+ y 0.5)] 0.08 :black)
        (sd/filled-circle sd [(+ x 0.5) (+ y 0.5)]
                         0.05
                         (case prop
                           "all"   :green
                           "air"   :orange
                           "none"  :red))))
    (when show-grid-lines
      (sd/grid sd
              0
              0
              (tiled/tm-width  tiled-map)
              (tiled/tm-height tiled-map) 1 1 [1 1 1 0.5]))))

(def ^:private world-id :worlds/uf-caves)

(defn- generate-screen-ctx [{:keys [clojure.graphics/world-viewport] :as c} properties]
  (let [{:keys [tiled-map start-position]} (generate-level c
                                                           (c/build world-id))
        atom-data (current-data)]
    (tiled/dispose (:tiled-map @atom-data))
    (swap! atom-data assoc
           :tiled-map tiled-map
           ;:area-level-grid area-level-grid
           :start-position start-position)
    (show-whole-map! (:camera world-viewport) tiled-map)
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)))

(defn ->generate-map-window [c level-id]
  (ui/window {:title "Properties"
              :cell-defaults {:pad 10}
              :rows [[(ui/label (with-out-str (pprint (c/build c level-id))))]
                     [(text-button "Generate" #(try (generate-screen-ctx c (c/build c level-id))
                                                    (catch Throwable t
                                                      (c/error-window @state t)
                                                      (println t))))]]
              :pack? true}))

(def ^:private zoom-speed 0.025)

(defn adjust-zoom [camera] ; TODO this now in clojure.context available.
  (when (input/key-pressed? :minus)  (cam/inc-zoom camera    zoom-speed))
  (when (input/key-pressed? :equals) (cam/inc-zoom camera (- zoom-speed))))

(defn enter [_]
  #_(show-whole-map! c/camera (:tiled-map @current-data)))

(defn exit [_]
  #_(cam/reset-zoom! c/camera))

(defn render [_]
  #_(draw-tiled-map @state
                    (:tiled-map @current-data)
                    (constantly color/white))
  #_(c/draw-on-world-view @state
                          render-on-map)
  #_(if (input/key-just-pressed? :l)
      (swap! current-data update :show-grid-lines not))
  #_(if (input/key-just-pressed? :m)
      (swap! current-data update :show-movement-properties not))
  #_(adjust-zoom c/camera)
  #_(camera-controls c/camera))

#_(defn dispose [_]
  #_(dispose (:tiled-map @current-data)))

(comment
 (atom {:tiled-map (tmx-map-loader/load modules/file)
        :show-movement-properties false
        :show-grid-lines false})
 )

#_(defn actors [_]
    [(->generate-map-window c world-id)
     (->info-window c)])
