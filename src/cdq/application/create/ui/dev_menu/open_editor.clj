(ns cdq.application.create.ui.dev-menu.open-editor
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.ui.editor.window]
            [clojure.string :as str]))

(defn menu [db]
  {:label "Editor"
   :items (for [property-type (sort (db/property-types db))]
            {:label (str/capitalize (name property-type))
             :on-click (fn [_actor ctx]
                         (ctx/open-editor-overview! ctx
                                                    {:property-type property-type
                                                     :clicked-id-fn (fn [_actor id {:keys [ctx/db] :as ctx}]
                                                                      (cdq.ui.editor.window/add-to-stage! ctx
                                                                                                          (db/get-raw db id)))}))})})
