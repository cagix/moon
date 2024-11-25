(ns ^:no-doc forge.schema.sound
  (:require [clojure.string :as str]
            [forge.assets :as assets :refer [play-sound]]
            [forge.schema :as schema]
            [forge.editor.widget :as widget]
            [forge.ui :as ui]
            [forge.ui.actor :as a]
            [forge.app :refer [add-actor]]
            [forge.editor.scrollpane :refer [scrollable-choose-window]]))

(defmethod schema/form :s/sound [_] :string)

(defn- play-button [sound-file]
  (ui/text-button "play!" #(play-sound sound-file)))

(declare columns)

(defn- choose-window [table]
  (let [rows (for [sound-file (assets/all-sounds)]
               [(ui/text-button (str/replace-first sound-file "sounds/" "")
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-file)])
                                  (a/remove! (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (.getUserObject table)]
                                    (.setUserObject table [k sound-file]))))
                (play-button sound-file)])]
    (add-actor (scrollable-choose-window rows))))

(defn- columns [table sound-file]
  [(ui/text-button (name sound-file) #(choose-window table))
   (play-button sound-file)])

(defmethod widget/create :s/sound [_ sound-file]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (columns table sound-file)
                           [(ui/text-button "No sound" #(choose-window table))])])
    table))
