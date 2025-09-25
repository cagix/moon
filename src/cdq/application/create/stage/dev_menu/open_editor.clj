(ns cdq.application.create.stage.dev-menu.open-editor
  (:require [cdq.db :as db]
            [cdq.ui.editor.window]
            [gdl.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [clojure.string :as str]))

(defn menu [db]
  {:label "Editor"
   :items (for [property-type (sort (db/property-types db))]
            {:label (str/capitalize (name property-type))
             :on-click (fn [_actor {:keys [ctx/db
                                           ctx/graphics
                                           ctx/stage]}]
                         (stage/add!
                          stage
                          (scene2d/build
                           {:actor/type :actor.type/editor-overview-window
                            :db db
                            :graphics graphics
                            :property-type property-type
                            :clicked-id-fn (fn [_actor id {:keys [ctx/db] :as ctx}]
                                             (cdq.ui.editor.window/add-to-stage!
                                              ctx
                                              (db/get-raw db id)))})))})})
