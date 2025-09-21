# Modules world spider tries to slow down projectile which deosnt have stats

java.lang.IllegalArgumentException: No implementation of method: :add of protocol: #'cdq.stats/Stats found for class: nil
        clojure.lang.ExceptionInfo: Error handling transaction
    transaction: [:tx/mod-add
              #<Atom@38cde4f2:
                {:entity/body {#, #, #, #, #, #},
                 :entity/destroy-audiovisual :audiovisuals/hit-wall,
                 :entity/faction :good,
                 :entity/movement {#, #},
                 :cdq.impl.grid/touched-cells [# # # #],
                 :entity/delete-after-duration {#, #},
                 :entity/temp-modifier {#, #},
                 :entity/id 968,
                 :entity/projectile-collision {#, #, #},
                 :entity/image #:image{#, #},
                 ...}>
              #:modifier{:movement-speed #:op{:mult -0.5}}]
        clojure.lang.ExceptionInfo: entity-tick
    entity/id: 239
            k: :active-skill


            => effect checks better ...
