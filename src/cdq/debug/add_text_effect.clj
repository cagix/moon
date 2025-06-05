(comment

 (defn get-eid [id]
   (get @(:ctx/entity-ids @cdq.application/state)
        id))

 (let [eid (get-eid 152)]
   (swap! eid
          cdq.tx.add-text-effect/add-text-effect*
          "Hello World"
          @cdq.application/state))
 )
