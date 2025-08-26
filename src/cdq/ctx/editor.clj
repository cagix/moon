(ns cdq.ctx.editor
  (:require [cdq.application :as application]
            [cdq.db :as db]
            [cdq.property :as property]
            [cdq.ui.editor.overview-table]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [cdq.ui.error-window :as error-window]
            [clojure.input :as input]
            [cdq.app :as app]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.ui.table :as table]
            [gdx.ui :as ui]))

(defn- apply-context-fn [window f]
  (fn [{:keys [ctx/app ctx/stage] :as ctx}]
    (try (f ctx)
         (actor/remove! window)
         (catch Throwable t
           (app/pretty-pst app t)
           (stage/add! stage (error-window/create t))))))

; We are working with raw property data without fetching relationships and build
; otherwise at update! we would have to convert again back to edn
; for example at images/relationships
(defn- create-editor-window
  [props
   {:keys [ctx/db
           ctx/graphics]
    :as ctx}]
  (let [schema (get (:schemas db) (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema nil props ctx)
        save!   (apply-context-fn window (fn [{:keys [ctx/db]}]
                                           (swap! application/state update :ctx/db
                                                  db/update!
                                                  (widget/value schema nil widget (:schemas db)))))
        delete! (apply-context-fn window (fn [_ctx]
                                           (swap! application/state update :ctx/db
                                                  db/delete!
                                                  (:property/id props))))]
    (table/add-rows! window [[(scroll-pane/table-cell (:viewport/height (:ui-viewport graphics))
                                                      [[{:actor widget :colspan 2}]
                                                       [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]"
                                                                                (fn [_actor ctx]
                                                                                  (save! ctx)))
                                                         :center? true}
                                                        {:actor (ui/text-button "Delete"
                                                                                (fn [_actor ctx]
                                                                                  (delete! ctx)))
                                                         :center? true}]])]])
    (group/add! window {:actor/type :actor.type/actor
                        :act (fn [_this _delta {:keys [ctx/input]}]
                               (when (input/key-just-pressed? input :enter)
                                 (save! ctx)))})
    (.pack window)
    window))

(defn open-property-editor-window! [{:keys [ctx/stage]
                                     :as ctx}
                                    property]
  (stage/add! stage (create-editor-window property ctx)))

(defn open-editor-overview-window! [{:keys [ctx/stage] :as ctx} property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})
        on-clicked-id (fn [id {:keys [ctx/db] :as ctx}]
                        (open-property-editor-window! ctx (db/get-raw db id)))]
    (table/add! window (cdq.ui.editor.overview-table/create ctx
                                                            property-type
                                                            on-clicked-id))
    (.pack window)
    (stage/add! stage window)))
