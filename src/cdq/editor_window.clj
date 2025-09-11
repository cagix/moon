(ns cdq.editor-window
  (:require [cdq.db :as db]
            [cdq.property :as property]
            [cdq.stacktrace :as stacktrace]
            [cdq.stage]
            [cdq.schema :as schema]
            [cdq.ui.widget]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.window :as window]))

(defn- create* [{:keys [save-fn
                        delete-fn
                        scrollpane-height
                        widget]}]
  (let [with-window-close (fn [f]
                            (fn [actor {:keys [ctx/stage] :as ctx}]
                              (try (f ctx)
                                   (actor/remove! (window/find-ancestor actor))
                                   (catch Throwable t
                                     (stacktrace/pretty-print t)
                                     (stage/add! stage (actor/build
                                                        {:actor/type :actor.type/error-window
                                                         :throwable t}))))))
        clicked-save-fn   (with-window-close save-fn)
        clicked-delete-fn (with-window-close delete-fn)]
    {:actor/type :actor.type/window
     :title "[SKY]Property[]"
     :id :property-editor-window
     :modal? true
     :close-button? true
     :center? true
     :close-on-escape? true
     :rows [[(cdq.ui.widget/scroll-pane-cell scrollpane-height
                                             [[{:actor widget :colspan 2}]
                                              [{:actor {:actor/type :actor.type/text-button
                                                        :text "Save [LIGHT_GRAY](ENTER)[]"
                                                        :on-clicked clicked-save-fn}
                                                :center? true}
                                               {:actor {:actor/type :actor.type/text-button
                                                        :text "Delete"
                                                        :on-clicked clicked-delete-fn}
                                                :center? true}]])]]
     :actors [{:actor/type :actor.type/actor
               :act (fn [actor _delta {:keys [ctx/input] :as ctx}]
                      (when (input/key-just-pressed? input :enter)
                        (clicked-save-fn actor ctx)))}]
     :cell-defaults {:pad 5}
     :pack? true}))

(defn property-editor-window
  [{:keys [ctx/application-state
           ctx/db
           ctx/stage]
    :as ctx}
   property]
  (let [schema (get (:schemas db) (property/type property))
        widget (schema/create schema nil property ctx)]
    (create* {:save-fn (fn [{:keys [ctx/db]}]
                         (swap! application-state update :ctx/db
                                db/update!
                                (schema/value schema nil widget (:schemas db))))
              :delete-fn (fn [_ctx]
                           (swap! application-state update :ctx/db
                                  db/delete!
                                  (:property/id property)))
              :scrollpane-height (cdq.stage/viewport-height stage)
              :widget widget})))

(defn rebuild!
  [{:keys [ctx/db
           ctx/stage]
    :as ctx}]
  (let [window (:property-editor-window stage)
        map-widget-table (-> window
                             :scroll-pane
                             (group/find-actor "scroll-pane-table")
                             :map-widget)
        prop-value (schema/value [:s/map] nil map-widget-table (:schemas db))]
    (actor/remove! window)
    (stage/add! stage (actor/build (property-editor-window ctx prop-value)))))
