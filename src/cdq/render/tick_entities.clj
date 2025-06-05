(ns cdq.render.tick-entities
  (:require [cdq.entity :as entity]
            [cdq.ctx :as ctx]
            [cdq.ui.error-window :as error-window]
            [gdl.ui.stage :as stage]
            [cdq.utils :as utils]))

(defn- tick-entities!
  [{:keys [ctx/active-entities
           ctx/stage]
    :as ctx}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (ctx/handle-txs! ctx (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info (str "entity/id: " (entity/id @eid)) {} t)))))
   (catch Throwable t
     (utils/pretty-pst t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn do! [{:keys [ctx/paused?]
            :as ctx}]
  (if paused?
    ctx
    (do
     (tick-entities! ctx)
     ctx)))
