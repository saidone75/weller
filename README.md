# weller
Weller is like Alfresco out-of-process extensions but in Clojure.

## Outline
- Lean and readable code
- 100% pure Clojure
- 100% test coverage

## Usage
### Create and compose filters
Filters are used for selecting messages that will flow through a tap. Filtering can discriminate the message by matching
several conditions (e.g. a node has been created or deleted, an aspect has been added to a node, a property changed its
value, etc.).

Filters can be composed of a single predicate:
```clojure
(filters/event? events/node-updated)
```
or a composition of predicates using `every-pred` (logical AND) or `some-fn` (logical OR) Clojure built-in functions.

E.g. this matches every updated node which is a file:
```clojure
(every-pred (filters/event? events/node-updated)
            (filters/is-file?))
```
this matches every node with `cm:titled` **or** `cm:dublincore` aspects (regardless of the event type): 
```clojure
(some-fn (filters/node-aspect? cm/asp-titled)
         (filters/node-aspect? cm/asp-dublincore))
```
and this matches updated nodes with `cm:titled` **or** `cm:dublincore` aspects:
```clojure
(every-pred (filters/event? events/node-updated)
            (some-fn (filters/node-aspect? cm/asp-titled)
                     (filters/node-aspect? cm/asp-dublincore)))
```
### Create a function
A (processing) function is the piece of code deputed to take the (resource part of) message and do something with it.
The (node) resource is a map representing (usually) a node in Alfresco.

A simple function that prints the node name could be:
```clojure
(defn print-node-name
  [resource]
  (println (:name resource)))
```
### Build and start a message pipe
```clojure
(-> (pipe/make-pipe)
    (pipe/add-tap (filters/event? events/node-created) process-created-node)
    (pipe/add-tap (filters/event? events/node-deleted) process-deleted-node)
    (component/start))
```
```clojure
(pipe/make-pipe (filters/event? events/node-created) process-created-node)
```
...
## License
Copyright (c) 2024 Saidone

Distributed under the GNU General Public License v3.0
