# weller
Weller is like Alfresco out-of-process extensions but in Clojure.

## Outline
- Lean and readable code
- 100% pure Clojure
- 100% test coverage

## Usage
### Create and compose filters
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
this matches every node with `cm:titled` **OR** `cm:dublincore` aspects (regardless of the event type): 
```clojure
(some-fn (filters/node-aspect? cm/asp-titled)
         (filters/node-aspect? cm/asp-dublincore))
```
and this matches updated nodes with `cm:titled` **OR** `cm:dublincore` aspects:
```clojure
(every-pred (filters/event? events/node-updated)
            (some-fn (filters/node-aspect? cm/asp-titled)
                     (filters/node-aspect? cm/asp-dublincore)))
```
### Create a function
...
### Build and start an event handler
...
## License
Copyright (c) 2024 Saidone

Distributed under the GNU General Public License v3.0
