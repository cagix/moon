(ns ^:no-doc core.property.data.sound
  (:require [clojure.string :as str]
            [core.ctx :refer :all]
            [core.property :as property]
            [core.ui :as ui]))

(defcomponent :sound {:schema :string})

(defn- ->scrollable-choose-window [ctx rows]
  (ui/->window {:title "Choose"
                :modal? true
                :close-button? true
                :center? true
                :close-on-escape? true
                :rows [[(ui/->scroll-pane-cell ctx rows)]]
                :pack? true}))

(defn- ->play-sound-button [sound-file]
  (ui/->text-button "play!" #(play-sound! % sound-file)))

(declare ->sound-columns)

(defn- open-sounds-window! [ctx table]
  (let [rows (for [sound-file (:sound-files (assets ctx))]
               [(ui/->text-button (str/replace-first sound-file "sounds/" "")
                                  (fn [{:keys [context/actor] :as ctx}]
                                    (ui/clear-children! table)
                                    (ui/add-rows! table [(->sound-columns table sound-file)])
                                    (ui/remove! (ui/find-ancestor-window actor))
                                    (ui/pack-ancestor-window! table)
                                    (ui/set-id! table sound-file)
                                    ctx))
                (->play-sound-button sound-file)])]
    (ui/stage-add! ctx (->scrollable-choose-window ctx rows))))

(defn- ->sound-columns [table sound-file]
  [(ui/->text-button (name sound-file) #(open-sounds-window! % table))
   (->play-sound-button sound-file)])

(defmethod property/->widget :sound [_ sound-file _ctx]
  (let [table (ui/->table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (->sound-columns table sound-file)
                           [(ui/->text-button "No sound" #(open-sounds-window! % table))])])
    table))
