(ns cdq.ui.editor.widget.sound
  (:require [cdq.ctx :as ctx]
            [cdq.tx.sound :as tx.sound]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.ui :as ui]))

(defn- play-button [sound-name]
  (ui/text-button "play!" #(tx.sound/do! (ctx/make-map) sound-name)))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- choose-window [table]
  (let [rows (for [sound-name (map sound-file->sound-name (assets/all-of-type ctx/assets :sound))]
               [(ui/text-button sound-name
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-name)])
                                  (.remove (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (ui/user-object table)]
                                    (ui/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (ui/add! ctx/stage (scroll-pane/choose-window rows))))

(defn- columns [table sound-name]
  [(ui/text-button sound-name #(choose-window table))
   (play-button sound-name)])

(defmethod widget/create :s/sound [_ sound-name]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-name
                           (columns table sound-name)
                           [(ui/text-button "No sound" #(choose-window table))])])
    table))
