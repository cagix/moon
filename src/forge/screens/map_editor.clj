(ns ^:no-doc forge.screens.map-editor
  (:require [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.input :refer [key-just-pressed?
                                       key-pressed?]]
            [clojure.gdx.tiled :as tiled]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [forge.app.cached-map-renderer :refer [draw-tiled-map]]
            [forge.app.db :as db]
            [forge.app.gui-viewport :refer [gui-viewport-height]]
            [forge.core :refer :all]
            [forge.controls :as controls]
            [forge.mapgen.modules :as modules]))

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
  (-> (current-screen) :sub-screen :current-data))

(def ^:private infotext
  "L: grid lines
M: movement properties
zoom: minus,equals
ESCAPE: leave
direction keys: move")

(defn- map-infos ^String []
  (let [tile (mapv int (world-mouse-position))
        {:keys [tiled-map
                area-level-grid]} @(current-data)]
    (->> [infotext
          (str "Tile " tile)
          (when-not area-level-grid
            (str "Module " (mapv (comp int /)
                                 (world-mouse-position)
                                 [modules/width modules/height])))
          (when area-level-grid
            (str "Creature id: " (tiled/property-value tiled-map :creatures tile :id)))
          (when area-level-grid
            (let [level (get area-level-grid tile)]
              (when (number? level)
                (str "Area level:" level))))
          (str "Movement properties " (movement-property tiled-map tile) "\n"
               (apply vector (movement-properties tiled-map tile)))]
         (remove nil?)
         (str/join "\n"))))

(defn- ->info-window []
  (let [label (label "")
        window (ui-window {:title "Info" :rows [[label]]})]
    (add-actor! window (ui-actor {:act #(do
                                         (.setText label (map-infos))
                                         (.pack window))}))
    (.setPosition window 0 gui-viewport-height)
    window))

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
        visible-tiles (cam/visible-tiles (world-camera))
        [x y] (mapv int (world-mouse-position))]
    (draw-rectangle x y 1 1 white)
    (when start-position
      (draw-filled-rectangle (start-position 0) (start-position 1) 1 1 [1 0 1 0.9]))
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [prop (movement-property tiled-map [x y])]]
        (draw-filled-circle [(+ x 0.5) (+ y 0.5)] 0.08 black)
        (draw-filled-circle [(+ x 0.5) (+ y 0.5)]
                          0.05
                          (case prop
                            "all"   :green
                            "air"   :orange
                            "none"  :red))))
    (when show-grid-lines
      (draw-grid 0
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
    (show-whole-map! (world-camera) tiled-map)
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)))

(defn ->generate-map-window [level-id]
  (ui-window {:title "Properties"
              :cell-defaults {:pad 10}
              :rows [[(label (with-out-str (pprint (db/build level-id))))]
                     [(text-button "Generate" #(try (generate-screen-ctx (db/build level-id))
                                                    (catch Throwable t
                                                      (error-window! t)
                                                      (println t))))]]
              :pack? true}))

(defrecord MapEditorScreen [current-data]
  Screen
  (screen-enter [_]
    (show-whole-map! (world-camera) (:tiled-map @current-data)))

  (screen-exit [_]
    (cam/reset-zoom! (world-camera)))

  (screen-render [_]
    (draw-tiled-map (:tiled-map @current-data)
                    (constantly white))
    (draw-on-world-view render-on-map)
    (if (key-just-pressed? :keys/l)
      (swap! current-data update :show-grid-lines not))
    (if (key-just-pressed? :keys/m)
      (swap! current-data update :show-movement-properties not))
    (controls/world-camera-zoom)
    (camera-controls (world-camera))
    (when (key-just-pressed? :keys/escape)
      (change-screen :screens/main-menu)))

  (screen-destroy [_]
    (dispose (:tiled-map @current-data))))

(defn create []
  {:actors [(->generate-map-window world-id)
            (->info-window)]
   :screen (->MapEditorScreen (atom {:tiled-map (tiled/load-tmx-map modules/file)
                                     :show-movement-properties false
                                     :show-grid-lines false}))})
