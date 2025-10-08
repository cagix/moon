(ns cdq.db.schema.sound
  (:require [cdq.audio :as sounds]
            [cdq.ui :as ui]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.ui.table :as table]
            [cdq.ui.window :as window])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn malli-form [_ _schemas]
  :string)

(defn create-value [_ v _db]
  v)

(declare sound-columns)

(defn- rebuild! [table sound-name]
  (fn [actor _ctx]
    (.clearChildren table)
    (table/add-rows! table [(sound-columns table sound-name)])
    (Actor/.remove (window/find-ancestor actor))
    (.pack (window/find-ancestor table))
    (let [[k _] (Actor/.getUserObject table)]
      (Actor/.setUserObject table [k sound-name]))))

(defn- open-select-sounds-handler [table]
  (fn [_actor {:keys [ctx/audio
                      ctx/stage]}]
    (.addActor stage
               (scene2d/build
                {:actor/type :actor.type/scroll-pane-window
                 :viewport-height (ui/viewport-width stage)
                 :rows (for [sound-name (sounds/sound-names audio)]
                         [{:actor {:actor/type :actor.type/text-button
                                   :text sound-name
                                   :on-clicked (rebuild! table sound-name)}}
                          {:actor {:actor/type :actor.type/text-button
                                   :text "play!"
                                   :on-clicked (fn [_actor {:keys [ctx/audio]}]
                                                 (sounds/play! audio sound-name))}}])}))))

(defn- sound-columns [table sound-name]
  [{:actor {:actor/type :actor.type/text-button
            :text sound-name
            :on-clicked (open-select-sounds-handler table)}}
   {:actor {:actor/type :actor.type/text-button
            :text "play!"
            :on-clicked (fn [_actor {:keys [ctx/audio]}]
                          (sounds/play! audio sound-name))}}])

(defn create [_  sound-name _ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (sound-columns table sound-name)
                              [{:actor {:actor/type :actor.type/text-button
                                        :text "No sound"
                                        :on-clicked (open-select-sounds-handler table)}}])])
    table))
