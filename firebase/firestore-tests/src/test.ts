import {assertFails, assertSucceeds} from "@firebase/rules-unit-testing";
import {FirebaseError} from "@firebase/util";
import {DocumentReference, DocumentSnapshot} from "firebase/firestore";
import chalk from "chalk";

const FIREBASE_ASSERT_FAILS_SUCCEEDED_MESSAGE =
  "Expected request to fail, but it succeeded.";


const stats = {
  successes: 0,
  failures: 0,
  errors: 0,
};

export function resetStats() {
  stats.successes = 0;
  stats.failures = 0;
  stats.errors = 0;
}

export async function test(
  testName: string,
  operationPromise: Promise<void | DocumentReference | DocumentSnapshot>,
  expectsSuccess: boolean,
) {
  console.log(chalk.yellow(`\nRUNNING: ${testName}`));

  const asserter = expectsSuccess ? assertSucceeds : assertFails;
  const testNotPassedMessage = expectsSuccess ?
    "Expected success but got failure" :
    "Expected failure but got success";

  try {
    await asserter(operationPromise);
  } catch (error) {
    if (!(error instanceof Error)) {
      console.error(
        "Received the following unexpected non-Error thing as an error."
      );
      console.error(error);

      console.log(chalk.bgRedBright("ERRORED"));
      stats.errors += 1;
      return;
    }

    if (
      // If we expected success, but got a failure, we should see a
      // FirebaseError.
      !(error instanceof FirebaseError) &&
      // Unfortunately, if we expected failure but got success we just see an
      // Error, of no particular type (likely because the very problem is that
      // there was no FirebaseError), however, the message of the error does
      // still allow us to detect it.
      error.message !== FIREBASE_ASSERT_FAILS_SUCCEEDED_MESSAGE
    ) {
      // We got an unexpected error.

      console.error("Received the following unexpected error.");
      console.error(error);

      console.log(chalk.bgRedBright("ERRORED"));
      stats.errors += 1;
      return;
    }

    console.error(error);

    console.log(chalk.red(`FAILED: ${testNotPassedMessage}`));
    stats.failures += 1;
    return;
  }

  console.log(chalk.green("TEST PASSED"));
  stats.successes += 1;
}

export function logStats() {
  console.log();
  console.log(`${stats.successes} successes`);
  console.log(`${stats.failures} failures`);
  console.log(`${stats.errors} errors`);
}

