(ns cdq.tx.open-property-editor
  (:require [cdq.db :as db]
            [cdq.editor-window]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]))

(defn do!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}
   property-id]
  (stage/add! stage (actor/build (cdq.editor-window/property-editor-window ctx (db/get-raw db property-id)))))
