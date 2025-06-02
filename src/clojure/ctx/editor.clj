(ns clojure.ctx.editor
  (:require [clojure.db :as db]
            [clojure.ui :as ui]
            [clojure.ui.editor]
            [clojure.ui.editor.overview-table]
            [clojure.ui.stage :as stage]))

(defn open-property-editor-window! [{:keys [ctx/stage]
                                     :as ctx}
                                    property]
  (stage/add! stage (clojure.ui.editor/create-editor-window property ctx)))

(defn open-editor-overview-window! [{:keys [ctx/stage] :as ctx} property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})
        on-clicked-id (fn [id {:keys [ctx/db] :as ctx}]
                        (open-property-editor-window! ctx (db/get-raw db id)))]
    (ui/add! window (clojure.ui.editor.overview-table/create ctx
                                                             property-type
                                                             on-clicked-id))
    (.pack window)
    (stage/add! stage window)))
