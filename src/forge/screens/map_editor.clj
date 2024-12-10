(ns ^:no-doc forge.screens.map-editor
  (:require [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.graphics.camera :as cam]
            [anvil.graphics.color :as color]
            [anvil.input :refer [key-just-pressed? key-pressed?]]
            [anvil.level :refer [generate-level]]
            [anvil.modules :as modules]
            [anvil.screen :as screen]
            [anvil.stage :as stage]
            [anvil.ui :refer [ui-actor text-button] :as ui]
            [anvil.utils :refer [dispose]]
            [anvil.world :as world]
            [anvil.ui.group :refer [add-actor!]]
            [anvil.tiled :as tiled]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]))

(defn- show-whole-map! [camera tiled-map]
  (cam/set-position! camera
                     [(/ (tiled/tm-width  tiled-map) 2)
                      (/ (tiled/tm-height tiled-map) 2)])
  (cam/set-zoom! camera
                 (cam/calculate-zoom camera
                                     :left [0 0]
                                     :top [0 (tiled/tm-height tiled-map)]
                                     :right [(tiled/tm-width tiled-map) 0]
                                     :bottom [0 0])))

(defn- current-data [] ; TODO just use vars
  (-> ((screen/current) 1)
      :sub-screen
      :current-data))

(def ^:private infotext
  "L: grid lines
  M: movement properties
  zoom: minus,equals
  ESCAPE: leave
  direction keys: move")

(defn- map-infos ^String []
  (let [tile (mapv int (world/mouse-position))
        {:keys [tiled-map
                area-level-grid]} @(current-data)]
    (->> [infotext
          (str "Tile " tile)
          (when-not area-level-grid
            (str "Module " (mapv (comp int /)
                                 (world/mouse-position)
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

(defn- ->info-window []
  (let [label (ui/label "")
        window (ui/window {:title "Info" :rows [[label]]})]
    (add-actor! window (ui-actor {:act #(do
                                         (.setText label (map-infos))
                                         (.pack window))}))
    (.setPosition window 0 ui/viewport-height) window))

(def ^:private camera-movement-speed 1)

; TODO textfield takes control !
; PLUS symbol shift & = symbol on keyboard not registered
(defn- camera-controls [camera]
  (let [apply-position (fn [idx f]
                         (cam/set-position! camera
                                            (update (cam/position camera)
                                                    idx
                                                    #(f % camera-movement-speed))))]
    (if (key-pressed? :keys/left)  (apply-position 0 -))
    (if (key-pressed? :keys/right) (apply-position 0 +))
    (if (key-pressed? :keys/up)    (apply-position 1 +))
    (if (key-pressed? :keys/down)  (apply-position 1 -))))

(defn- render-on-map []
  (let [{:keys [tiled-map
                area-level-grid
                start-position
                show-movement-properties
                show-grid-lines]} @(current-data)
        visible-tiles (cam/visible-tiles (world/camera))
        [x y] (mapv int (world/mouse-position))]
    (g/rectangle x y 1 1 color/white)
    (when start-position
      (g/filled-rectangle (start-position 0) (start-position 1) 1 1 [1 0 1 0.9]))
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [prop (tiled/movement-property tiled-map [x y])]]
        (g/filled-circle [(+ x 0.5) (+ y 0.5)] 0.08 color/black)
        (g/filled-circle [(+ x 0.5) (+ y 0.5)]
                         0.05
                         (case prop
                           "all"   :green
                           "air"   :orange
                           "none"  :red))))
    (when show-grid-lines
      (g/grid 0
              0
              (tiled/tm-width  tiled-map)
              (tiled/tm-height tiled-map) 1 1 [1 1 1 0.5]))))

(def ^:private world-id :worlds/uf-caves)

(defn- generate-screen-ctx [properties]
  (let [{:keys [tiled-map start-position]} (generate-level (db/build world-id))
        atom-data (current-data)]
    (dispose (:tiled-map @atom-data))
    (swap! atom-data assoc
           :tiled-map tiled-map
           ;:area-level-grid area-level-grid
           :start-position start-position)
    (show-whole-map! (world/camera) tiled-map)
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)))

(defn ->generate-map-window [level-id]
  (ui/window {:title "Properties"
              :cell-defaults {:pad 10}
              :rows [[(ui/label (with-out-str (pprint (db/build level-id))))]
                     [(text-button "Generate" #(try (generate-screen-ctx (db/build level-id))
                                                    (catch Throwable t
                                                      (stage/error-window! t)
                                                      (println t))))]]
              :pack? true}))

(defn enter [_]
  #_(show-whole-map! (world/camera) (:tiled-map @current-data)))

(defn exit [_]
  #_(cam/reset-zoom! (world/camera)))

(defn render [_]
  #_(world/draw-tiled-map (:tiled-map @current-data)
                          (constantly color/white))
  #_(world/draw-on-view render-on-map)
  #_(if (key-just-pressed? :keys/l)
      (swap! current-data update :show-grid-lines not))
  #_(if (key-just-pressed? :keys/m)
      (swap! current-data update :show-movement-properties not))
  #_(controls/adjust-zoom (world/camera))
  #_(camera-controls (world/camera))
  #_(when (key-just-pressed? :keys/escape)
      (screen/change :screens/main-menu)))

(defn dispose [_]
  #_(dispose (:tiled-map @current-data)))

(comment
 (atom {:tiled-map (tiled/load-tmx-map modules/file)
        :show-movement-properties false
        :show-grid-lines false})
 )

(defn actors [_]
  [(->generate-map-window world-id)
   (->info-window)])
