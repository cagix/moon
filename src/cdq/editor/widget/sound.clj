(ns cdq.editor.widget.sound
  (:require [cdq.editor.scroll-pane :as scroll-pane]
            [cdq.audio :as audio]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [cdq.ui.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui :as ui]))

(defn- play-button [sound-name]
  (text-button/create "play!"
                      (fn [_actor {:keys [ctx/audio]}]
                        (audio/play-sound! audio sound-name))))

(declare columns)

(defn- open-choose-sound-window! [table
                                  {:keys [ctx/audio
                                          ctx/stage
                                          ctx/ui-viewport]}]
  (let [rows (for [sound-name (audio/all-sounds audio)]
               [(text-button/create sound-name
                                    (fn [actor _ctx]
                                      (group/clear-children! table)
                                      (table/add-rows! table [(columns table sound-name)])
                                      (.remove (actor/find-ancestor-window actor))
                                      (actor/pack-ancestor-window! table)
                                      (let [[k _] (actor/user-object table)]
                                        (actor/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add! stage (scroll-pane/choose-window (:viewport/width ui-viewport)
                                                 rows))))

(defn- columns [table sound-name]
  [(text-button/create sound-name
                       (fn [_actor ctx]
                         (open-choose-sound-window! table ctx)))
   (play-button sound-name)])

(defn create [_  _attribute sound-name _ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (columns table sound-name)
                              [(text-button/create "No sound"
                                                   (fn [_actor ctx]
                                                     (open-choose-sound-window! table ctx)))])])
    table))
