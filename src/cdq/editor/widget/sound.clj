(ns cdq.editor.widget.sound
  (:require [cdq.audio :as audio]
            [cdq.ui.widget]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.vis-ui.widget :as widget]))

(defn- play-button [sound-name]
  (widget/text-button "play!"
                      (fn [_actor {:keys [ctx/audio]}]
                        (audio/play-sound! audio sound-name))))

(declare columns)

(defn- open-choose-sound-window! [table
                                  {:keys [ctx/audio
                                          ctx/stage
                                          ctx/ui-viewport]}]
  (let [rows (for [sound-name (audio/all-sounds audio)]
               [(widget/text-button sound-name
                                    (fn [actor _ctx]
                                      (group/clear-children! table)
                                      (table/add-rows! table [(columns table sound-name)])
                                      (.remove (window/find-ancestor actor))
                                      (window/pack-ancestors! table)
                                      (let [[k _] (actor/user-object table)]
                                        (actor/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add! stage (cdq.ui.widget/scroll-pane-window (:viewport/width ui-viewport)
                                                        rows))))

(defn- columns [table sound-name]
  [(widget/text-button sound-name
                       (fn [_actor ctx]
                         (open-choose-sound-window! table ctx)))
   (play-button sound-name)])

(defn create [_  _attribute sound-name _ctx]
  (let [table (widget/table {:cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (columns table sound-name)
                              [(widget/text-button "No sound"
                                                   (fn [_actor ctx]
                                                     (open-choose-sound-window! table ctx)))])])
    table))
