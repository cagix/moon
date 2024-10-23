(ns ^:no-doc moon.editor.widgets
  (:require [clojure.edn :as edn]
            [gdl.assets :as gdx.assets]
            [clojure.string :as str]
            [moon.db :as db]
            [moon.info :as info]
            [moon.property :as property]
            [moon.editor.widget :as widget]
            [moon.editor.overview :refer [overview-table]]
            [moon.editor.utils :refer [scrollable-choose-window]]
            [moon.assets :as assets]
            [moon.audio :as audio]
            [moon.graphics :as g]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.stage :as stage]
            [gdl.utils :refer [truncate ->edn-str]])
  (:import (com.kotcrab.vis.ui.widget VisTextField VisCheckBox))
  (:load "widgets_relationships"))

(defn- add-schema-tooltip! [widget schema]
  (ui/add-tooltip! widget (str schema))
  widget)

;;

(defmethod widget/create :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod widget/value :default [_ widget]
  ((a/id widget) 1))

;;

(defmethod widget/create :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod widget/value :boolean [_ widget]
  (VisCheckBox/.isChecked  widget))

;;

(defmethod widget/create :string [schema v]
  (add-schema-tooltip! (ui/text-field v {}) schema))

(defmethod widget/value :string [_ widget]
  (VisTextField/.getText widget))

;;

(defmethod widget/create number? [schema v]
  (add-schema-tooltip! (ui/text-field (->edn-str v) {}) schema))

(defmethod widget/value number? [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

;;

(defmethod widget/create :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod widget/value :enum [_ widget]
  (edn/read-string (.getSelected ^com.kotcrab.vis.ui.widget.VisSelectBox widget)))

;;

(defn- all-textures []
  (gdx.assets/of-class assets/manager com.badlogic.gdx.graphics.Texture))

(defn- all-sounds []
  (gdx.assets/of-class assets/manager com.badlogic.gdx.audio.Sound))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows []
  (for [file (sort (all-textures))]
    [(ui/image-button (g/image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defn- big-image-button [image]
  (ui/image-button (g/edn->image image)
                   (fn on-clicked [])
                   {:scale 2}))

(defmethod widget/create :s/image [_ image]
  (big-image-button image)
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here

;;

(defmethod widget/create :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (big-image-button image))]
             :cell-defaults {:pad 1}}))

;;


(defn- ->play-sound-button [sound-file]
  (ui/text-button "play!" #(audio/play-sound! sound-file)))

(declare ->sound-columns)

(defn- open-sounds-window! [table]
  (let [rows (for [sound-file (all-sounds)]
               [(ui/text-button (str/replace-first sound-file "sounds/" "")
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(->sound-columns table sound-file)])
                                  (a/remove! (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (a/set-id! table sound-file)))
                (->play-sound-button sound-file)])]
    (stage/add! (scrollable-choose-window rows))))

(defn- ->sound-columns [table sound-file]
  [(ui/text-button (name sound-file) #(open-sounds-window! table))
   (->play-sound-button sound-file)])

(defmethod widget/create :s/sound [_ sound-file]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (->sound-columns table sound-file)
                           [(ui/text-button "No sound" #(open-sounds-window! table))])])
    table))
